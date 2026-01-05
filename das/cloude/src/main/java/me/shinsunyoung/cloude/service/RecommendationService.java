package me.shinsunyoung.cloude.service;

import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    public String getTodayOutfit(String region) {
        return "두꺼운 패딩 + 장갑 추천";
    }
}
