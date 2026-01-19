package me.shinsunyoung.projectweatherly.outfit.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OutfitSet {

    private List<String> outer;
    private List<String> top;
    private List<String> bottom;
}