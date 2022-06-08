package it.polimi.ingsw.utils.gui;


import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.ArrayList;
import java.util.List;

public class Converter {

    public static List<ImageView> toImage(List<String> stringList) {
        List<ImageView> images = new ArrayList<>();
        stringList.forEach((student) -> {
            switch (student) {
                case "rosso":
                    images.add(new ImageView("graphics/pieces/students/student_red.png"));
                    break;
                case "blu":
                    images.add(new ImageView("graphics/pieces/students/student_blue.png"));
                    break;
                case "verde":
                    images.add(new ImageView("graphics/pieces/students/student_green.png"));
                    break;
                case "giallo":
                    images.add(new ImageView("graphics/pieces/students/student_giallo.png"));
                    break;
            }
        });
        return images;
    }

    public static void toImage(List<String> stringList, List<ImageView> images) {
        for (int i = 0; i < 3; i++) {
            Image character = null;
            switch (stringList.get(i)) {
                case "MONK":
                    character = new Image("graphics/characters/monk.jpg");
                    break;
                case "PRINCESS":
                    character = new Image("graphics/characters/princess.jpg");
                    break;
                case "MUSHROOMHUNTER":
                    character = new Image("graphics/characters/mushroom_hunter.jpg");
                    break;
                case "THIEF":
                    character = new Image("graphics/characters/thief.jpg");
                    break;
                case "FARMER":
                    character = new Image("graphics/characters/farmer.jpg");
                    break;
                case "CENTAUR":
                    character = new Image("graphics/characters/centaur.jpg");
                    break;
                case "KNIGHT":
                    character = new Image("graphics/characters/knight.jpg");
                    break;
                case "POSTMAN":
                    character = new Image("graphics/characters/postman.jpg");
                    break;
                case "GRANNY":
                    character = new Image("graphics/characters/granny.jpg");
                    break;
                case "JESTER":
                    character = new Image("graphics/characters/jester.jpg");
                    break;
                case "HERALD":
                    character = new Image("graphics/characters/herald.jpg");
                    break;
                case "MINSTRELL":
                    character = new Image("graphics/characters/minstrell.jpg");
                    break;
            }
            images.get(i).setImage(character);
        }
       // return images;
    }

}