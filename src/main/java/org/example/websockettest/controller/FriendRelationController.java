//package org.example.websockettest.controller;
//
//import org.example.websockettest.entity.FriendRelationEntity;
//import org.example.websockettest.entity.UserEntity;
//import org.example.websockettest.repository.FriendRelationRepositoryImpl;
//import org.example.websockettest.repository.UserRepositoryImpl;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//@RestController
//@RequestMapping("/friend")
//public class FriendRelationController {
//    private final FriendRelationRepositoryImpl friendRelationRepository;
//    private final UserRepositoryImpl userRepository;
//
//    public FriendRelationController(FriendRelationRepositoryImpl friendRelationRepository, UserRepositoryImpl userRepository) {
//        this.friendRelationRepository = friendRelationRepository;
//        this.userRepository = userRepository;
//    }
//
//    @PostMapping("/check")
//    public boolean listCheck(Long user_id, Long friend_id){
//        //친구 목록에 있다면 false 리턴
//        if(friendRelationRepository.countByUserId1OrUserId2AndUserId2OrUserId1(user_id, friend_id, friend_id, user_id) == 1){
//            return false;
//        }
//
//        return true;
//    }
//
//    @GetMapping("/list/{jwt}")
//    public List<UserEntity> getList(@PathVariable String jwt){
//        //jwt 해체해서 user_id 가져와야함
//        List<FriendRelationEntity> relationList = friendRelationRepository.findByUserId1OrUserId2(user_id, user_id);
//
//        //열에서 내 값 지우고 친구 id만 뽑아 리스트 저장
//        List<Long> extractedFriendsId = relationList.stream()
//                .flatMap(relation -> {
//                    if (relation.getUserId1().equals(user_id)) {
//                        return Stream.of(relation.getUserId2());
//                    } else if (relation.getUserId2().equals(user_id)) {
//                        return Stream.of(relation.getUserId1());
//                    }
//                    return Stream.empty();
//                })
//                .collect(Collectors.toList());
//
//        List<UserEntity> friendList = new ArrayList<>();
//
//        //뽑아낸 id로 userEntity 리스트 만들어 저장
//        for(Long id : extractedFriendsId){
//            friendList.add(userRepository.findByuserId(id));
//        }
//
//        return friendList;
//    }
//}
