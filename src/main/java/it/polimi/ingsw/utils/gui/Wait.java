package it.polimi.ingsw.utils.gui;

import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

import static it.polimi.ingsw.client.GUI.currNode;
import static it.polimi.ingsw.client.GUI.messageToOthers;

public class Wait implements Initializable {
    public Label label = new Label();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currNode = label;
    }
}
