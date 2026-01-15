package me.shinsunyoung.projectweatherly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class OutfitController {

    private static final Logger logger = LoggerFactory.getLogger(OutfitController.class);

    @GetMapping("/outfit")
    public String outfitPage() {
        logger.info("옷차림 페이지 접속 요청");
        return "outfit"; // templates/outfit.html을 반환
    }

    @GetMapping("/outfit/detail")
    public String outfitDetailPage() {
        logger.info("옷차림 상세 페이지 접속 요청");
        return "outfit-detail";
    }

}