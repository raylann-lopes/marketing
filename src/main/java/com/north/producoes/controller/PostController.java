package com.north.producoes.controller;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.dto.response.PostResponse;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.service.PostService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    @GetMapping
    public ResponseEntity<List<PostResponse>> findAll(){
        List<PostResponse> post = postService.findAllPost()
                .stream()
                .map(PostResponse::from)
                .toList();
        return ResponseEntity.ok(post);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PostResponse>> findByStatus(@PathVariable @RequestParam PostStatusEnum status){
        List<PostResponse> postStatus = postService.findByStatus(status)
                .stream()
                .map(PostResponse::from)
                .toList();
        return ResponseEntity.ok(postStatus);

    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<PostResponse>> findByClient(@PathVariable @RequestParam ClientEntity clientId){
        List<PostResponse> post = postService.findByClient(clientId)
                .stream()
                .map(PostResponse::from)
                .toList();
        return ResponseEntity.ok(post);
    }

    @GetMapping("/user/{user}")
    public ResponseEntity<List<PostResponse>> findByUser(@PathVariable @RequestParam UserEntity user){
        List<PostResponse> postUser = postService.findByUser(user)
                .stream()
                .map(PostResponse::from)
                .toList();

        return ResponseEntity.ok(postUser);
    }

    @GetMapping("/scheduled/{scheduledAt}")
    public ResponseEntity<List<PostResponse>> findByScheduledAt(@PathVariable @RequestParam LocalDateTime scheduledAt, LocalDateTime scheduledAtBefore){
        List<PostResponse> postScheduledAt = postService.findByScheduledAt(scheduledAt, scheduledAtBefore)
                .stream()
                .map(PostResponse::from)
                .toList();
        return ResponseEntity.ok(postScheduledAt);
    }

    @PostMapping("/save")
    public ResponseEntity<PostResponse> savePost(@RequestBody PostEntity post){
        PostResponse postResponse = PostResponse.from(postService.savePost(post));
        return ResponseEntity.ok(postResponse);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<PostResponse> updatePost(@RequestBody PostEntity post){
        PostResponse postResponse = PostResponse.from(postService.updatePost(post));
        return ResponseEntity.ok(postResponse);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deletePostById(@PathVariable @RequestBody Long id) {
        postService.deletePostById(id);
        return ResponseEntity.noContent().build();
    }
}
