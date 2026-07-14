/**
 * Smoke de contrato da API financeira.
 *
 * Exercita as rotas reais que o financeService.ts consome — se o backend
 * renomear rota, método ou campo do DTO, este teste quebra na hora (a tela
 * de financeiro já quebrou em produção por deriva silenciosa: 405 em rota
 * que o frontend chamava com outro nome).
 *
 * Pré-requisitos: backend rodando e um usuário ADMIN válido.
 * Configuração (via variáveis de ambiente do Cypress):
 *   CYPRESS_apiUrl      — default http://localhost:8080
 *   CYPRESS_adminEmail  — e-mail do admin (obrigatório)
 *   CYPRESS_adminPassword — senha do admin (obrigatório)
 *
 * Exemplo:
 *   CYPRESS_adminEmail=admin@x.com CYPRESS_adminPassword=... npx cypress run --spec cypress/e2e/finance-contract.cy.ts
 */
describe('Contrato da API financeira', () => {
  const apiUrl = Cypress.env('apiUrl') || 'http://localhost:8080'
  const adminEmail = Cypress.env('adminEmail')
  const adminPassword = Cypress.env('adminPassword')

  let token = ''
  let clientId: number
  let createdId: number

  before(() => {
    expect(adminEmail, 'CYPRESS_adminEmail é obrigatório').to.be.a('string').and.not.be.empty
    expect(adminPassword, 'CYPRESS_adminPassword é obrigatório').to.be.a('string').and.not.be.empty

    cy.request('POST', `${apiUrl}/api/auth/login`, {
      email: adminEmail,
      password: adminPassword,
    }).then((res) => {
      expect(res.status).to.eq(200)
      token = res.body.token
    })

    cy.then(() =>
      cy.request({
        url: `${apiUrl}/api/clients`,
        headers: { Authorization: `Bearer ${token}` },
      }),
    ).then((res) => {
      expect(res.status).to.eq(200)
      expect(res.body, 'é preciso ao menos um cliente cadastrado').to.have.length.greaterThan(0)
      clientId = res.body[0].id
    })
  })

  it('POST /api/finance cria conta (201) com o payload do frontend', () => {
    cy.request({
      method: 'POST',
      url: `${apiUrl}/api/finance`,
      headers: { Authorization: `Bearer ${token}` },
      body: {
        clientId,
        description: '[cypress] conta de teste',
        value: 123.45,
        status: 'PENDING',
        type: 'VARIABLE_REVENUE',
        expirationDate: '2026-12-20',
        paymentDate: null,
      },
    }).then((res) => {
      expect(res.status).to.eq(201)
      expect(res.body.clientId).to.eq(clientId)
      expect(res.body.status).to.eq('PENDING')
      createdId = res.body.id
    })
  })

  it('PUT /api/finance/{id} marca como paga (200) e expõe paymentDate', () => {
    cy.request({
      method: 'PUT',
      url: `${apiUrl}/api/finance/${createdId}`,
      headers: { Authorization: `Bearer ${token}` },
      body: {
        clientId,
        description: '[cypress] conta de teste',
        value: 123.45,
        status: 'PAY',
        type: 'VARIABLE_REVENUE',
        expirationDate: '2026-12-20',
        paymentDate: '2026-12-18',
      },
    }).then((res) => {
      expect(res.status).to.eq(200)
      expect(res.body.status).to.eq('PAY')
      expect(res.body.paymentDate).to.contain('2026-12-18')
    })
  })

  it('GET /api/finance filtra por status e intervalo de vencimento', () => {
    cy.request({
      url: `${apiUrl}/api/finance?status=PAY&from=2026-12-01&to=2026-12-31`,
      headers: { Authorization: `Bearer ${token}` },
    }).then((res) => {
      expect(res.status).to.eq(200)
      const found = res.body.find((f: { id: number }) => f.id === createdId)
      expect(found, 'conta paga deve aparecer no filtro').to.exist
    })

    cy.request({
      url: `${apiUrl}/api/finance?status=PENDING&from=2026-12-01&to=2026-12-31`,
      headers: { Authorization: `Bearer ${token}` },
    }).then((res) => {
      const found = res.body.find((f: { id: number }) => f.id === createdId)
      expect(found, 'conta paga não deve aparecer como pendente').to.not.exist
    })
  })

  it('DELETE /api/finance/{id} exclui a conta (204)', () => {
    cy.request({
      method: 'DELETE',
      url: `${apiUrl}/api/finance/${createdId}`,
      headers: { Authorization: `Bearer ${token}` },
    }).then((res) => {
      expect(res.status).to.eq(204)
    })
  })
})
