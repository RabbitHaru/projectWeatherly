package me.shinsunyoung.projectweatherly.clothing.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ClothingSet {

    private List<String> outer;
    private List<String> top;
    private List<String> bottom;
}