package it.polimi.ingsw.server.model.characters;

import it.polimi.ingsw.server.model.CenterTable;

public abstract class CharacterCard {
    private String Name;
    private int cost;
    private final String descrition;
    public CharacterCard(String descrition,int cost, String name){
        this.Name= name;
        this.cost=cost;
        this.descrition="EFFETTO: "+descrition;
    }

    public int getCost() {
        return cost;
    }

    public void improveCost() {
        this.cost ++;
    }

    @Override
    public String toString() {
        return "COST: " + cost +
                "\n" + descrition ;
    }

    public String getName() {
        return Name;
    }
}
