package it.polimi.ingsw.client.view.cli;

import com.google.gson.Gson;
import it.polimi.ingsw.client.model.ClientModel;
import it.polimi.ingsw.client.controller.states.Decision;
import it.polimi.ingsw.client.controller.states.ReadFromTerminal;
import it.polimi.ingsw.client.controller.states.WelcomeScreen;
import it.polimi.ingsw.client.view.View;
import it.polimi.ingsw.server.model.AssistantCard;
import it.polimi.ingsw.server.model.StudentSet;
import it.polimi.ingsw.server.model.characters.*;
import it.polimi.ingsw.server.model.enums.GameMode;
import it.polimi.ingsw.server.model.enums.PeopleColor;
import it.polimi.ingsw.utils.cli.CommandPrompt;
import it.polimi.ingsw.utils.network.Network;
import it.polimi.ingsw.utils.stateMachine.State;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static it.polimi.ingsw.utils.common.Check.isValidIp;
import static it.polimi.ingsw.utils.common.Check.isValidPort;

public class CliView implements View{

    // La vista matiene una reference allo stato chiamante (schermata video/command line) ed al precedente.
    private State callingState;

    // parsedString ci serve per parsare l'input e verificare la correttezza dei dati inseriti
    private ArrayList<String> parsedStrings;

    private ClientModel networkClientModel;
    private String mynickname;
    private String responce;
    private boolean isUsed;
    private Gson json;
    public CliView(){
        responce="\n";
        isUsed=false;
    }
    public void setClientModel(ClientModel clientModel) {
        this.networkClientModel = clientModel;
    }

    // Uno stato che vuole chiamare un metodo della vista si registra prima chiamando questo metodo
    // ad esempio sono nello stato WelcomeScreen e faccio "view.setCallingState(this)"
    // Non è altro che il pattern Observer riadattato per il pattern State
    @Override
    public void setCallingState(State callingState) {
        this.callingState = callingState;
    }

    @Override
    public void askToStart() {
        if (callingState instanceof WelcomeScreen) {
            ((WelcomeScreen) callingState).start().enable();
            ((WelcomeScreen) callingState).notStart().enable();

            CommandPrompt.ask(
                    "Scrivi start per far partire il gioco e premi invio",
                    "START THE GAME> ");


            ((WelcomeScreen) callingState).start().disable();
            ((WelcomeScreen) callingState).notStart().disable();
        }
    }

    @Override
    public void askDecision(String option1, String option2) {
        if (callingState instanceof Decision) {
            ((Decision) callingState).haScelto1().enable();
            ((Decision) callingState).haScelto2().enable();

            CommandPrompt.ask(
                    "Scegli tra " + option1 + " e " + option2,
                    option1 + " or " + option2+"> ");

            ((Decision) callingState).haScelto1().disable();
            ((Decision) callingState).haScelto2().disable();
        }
    }

    /**
     * Metodo di richiesta all'utente di inserimento di un certo numero di parole separate da uno spazio,
     * lo stato chiamante sarà uno stato di Read (lettura da terminale), lo stato si occupa solo del dichiarare
     * il numero di parole da leggere e di recuperare poi le informazioni mettendole nel ClientModel, oggetto che
     * verrà poi spedito al server.
     * E' possibile implementare anche controlli client side di correttezza dei parametri inseriti, ad esempio se un
     * indirizzo IP è valido o meno.
     */
    @Override
    public void askParameters() {
        ((ReadFromTerminal) callingState).numberOfParametersIncorrect().enable();
        ((ReadFromTerminal) callingState).insertedParameters().enable();

        switch (((ReadFromTerminal) callingState).getType()){

            case "USERINFO":
                CommandPrompt.ask("Inserire Nickname Ip e porta separati da uno spazio e premere invio",
                                      "nickname ip porta>");
                // Controllo di correttezza dei dati inseriti lato client
                parsedStrings =
                        new ArrayList<String>(Arrays.asList(CommandPrompt.gotFromTerminal().split(" ")));
                if (parsedStrings.size()!=3 || !isValidIp(parsedStrings.get(1)) || !isValidPort(parsedStrings.get(2))){
                    System.out.print("ALERT: dati inseriti non validi, riprovare\n");
                    askParameters();
                }
                setMynickname(parsedStrings.get(0));
                break;

            case "GAMEINFO" :
                CommandPrompt.ask("Inserire numero di giocatori e modalità di gioco ",
                        "numOfPlayers gameMode>");
                // Controllo di correttezza dei dati inseriti lato client
                parsedStrings =
                        new ArrayList<String>(Arrays.asList(CommandPrompt.gotFromTerminal().split(" ")));
                // controllo che numOfPlayers sia una cifra
                if (!isValidCifra(parsedStrings.get(0))){
                    System.out.print("ALERT: Inserisci una cifra su numOfPlayers, riprovare\n");
                    askParameters();
                    return;
                }
                // se è una cifra allora deve essere compresa tra 2 e 4
                if(Integer.parseInt(parsedStrings.get(0))<2 || Integer.parseInt(parsedStrings.get(0))>4){
                    System.out.print("ALERT: numOfPlayers deve essere compresa tra 2 e 4, riprovare\n");
                    askParameters();
                    return;
                }
                // controllo che il gamemode sia tra PRINCIPIANT ed EXPERT
                if (!parsedStrings.get(1).equals("PRINCIPIANT") && !parsedStrings.get(1).equals("EXPERT")){
                    System.out.print("ALERT: il gameMode scelto deve essere 'PRINCIPIANT' oppure 'EXPERT', riprovare\n");
                    askParameters();
                    return;
                }
                // Se siamo arrivati quì l'utente ha inserito numero di giocatori e gamemode nel formato richiesto
                // ovvero 2, 3, o 4 per numOfPlayers e PRINCIPIANT o EXPERT per la modalità di gioco
                // non occorre fare ulteriori controlli.
                break;

            case "NICKNAMEEXISTENT" :
                CommandPrompt.ask("Il nickname scelto è già esistente, si prega di reinserirne uno nuovo",
                        "nickname>");
                setMynickname(CommandPrompt.gotFromTerminal());
                break;
        }

        ((ReadFromTerminal) callingState).numberOfParametersIncorrect().disable();
        ((ReadFromTerminal) callingState).insertedParameters().disable();
    }

    // Il server mi invia una richiesta di interazione: devo digitare roba da terminale
    public void requestToMe() throws InterruptedException {
        switch(networkClientModel.getTypeOfRequest()){

            case "CHOOSEASSISTANTCARD" :
                System.out.println(networkClientModel.getServermodel().toString(networkClientModel.getNickname(),"STATO DEL GIOCO: \n"+"E' IL TUO TURNO - ASSISTENT CARD PHASE"+ "\n\nMOSSE ALTRI GIOCATORI: "+getResponce()));
                System.out.println("Scegli una Carta Assistente");
                for (AssistantCard a : networkClientModel.getDeck()){;
                    System.out.println("valore: " + a.getValues() + "  mosse: " + a.getMoves());
                }
                CommandPrompt.ask("Inserisci valore della carta scelta","Carta> ");

                if(!isValidNumber(CommandPrompt.gotFromTerminal())){
                    System.out.println("la carta da te scelta ha un valore non  valido, si prega di fare più attenzione");
                    TimeUnit.SECONDS.sleep(2);
                    requestToMe();
                    return;
                }
                boolean check=false;
                for(int i=0;i<networkClientModel.getDeck().size();i++){
                    if(networkClientModel.getDeck().get(i).getValues()==Integer.parseInt((CommandPrompt.gotFromTerminal()))){
                        check=true;
                    }
                }
                if(!check){
                    System.out.println("la carta scelta non è presente, fare più attenzione");
                    TimeUnit.SECONDS.sleep(2);
                    requestToMe();
                    return;
                }
                networkClientModel.setCardChoosedValue(Float.parseFloat(CommandPrompt.gotFromTerminal()));
                networkClientModel.setResponse(true); //lo flaggo come messaggio di risposta
                networkClientModel.setFromTerminal(parsedStrings);
                json = new Gson();
                Network.send(json.toJson(networkClientModel));

                break;
            case "CHOOSEWHERETOMOVESTUDENTS" :
                if(networkClientModel.getServermodel().getGameMode().equals(GameMode.EXPERT) && networkClientModel.getServermodel().getTable().getCards().stream().anyMatch(j->j.getCost()<= networkClientModel.getServermodel().getcurrentPlayer().getCoins() && !isUsed)) {
                    System.out.println(networkClientModel.getServermodel().toString(networkClientModel.getNickname(), "STATO DEL GIOCO: \n" + "E' IL TUO TURNO - STUDENT PHASE" + "\n\nMOSSE ALTRI GIOCATORI: " + getResponce()));

                    CommandPrompt.ask("Scegli il colore dello studente che desideri muovere OPPURE inserisci CARD per usare una carta personaggio", "RED or GREEN or BLUE or YELLOW or PINK    or CARD> ");

                    if (!CommandPrompt.gotFromTerminal().equals("RED") &&
                            !CommandPrompt.gotFromTerminal().equals("GREEN") &&
                            !CommandPrompt.gotFromTerminal().equals("BLUE") &&
                            !CommandPrompt.gotFromTerminal().equals("YELLOW") &&
                            !CommandPrompt.gotFromTerminal().equals("PINK") &&
                            !CommandPrompt.gotFromTerminal().equals("CARD")) {
                        System.out.println("Non si è inserito ne un colore non valido, ne il comando CARD, reinserire i dati con più attenzione !!!!");
                        TimeUnit.SECONDS.sleep(2);
                        requestToMe();
                        return;
                    }

                    PeopleColor choosedColor = null;

                    switch (CommandPrompt.gotFromTerminal()) {
                        case "RED":
                            choosedColor = PeopleColor.RED;
                            break;
                        case "GREEN":
                            choosedColor = PeopleColor.GREEN;
                            break;
                        case "BLUE":
                            choosedColor = PeopleColor.BLUE;
                            break;
                        case "YELLOW":
                            choosedColor = PeopleColor.YELLOW;
                            break;
                        case "PINK":
                            choosedColor = PeopleColor.PINK;
                            break;
                        default:
                            System.out.println("Scegli la carta che vorresti utilizzare: \n");
                            ArrayList<String> avaiable = new ArrayList<>();
                            for (int i = 0; i < networkClientModel.getServermodel().getTable().getCards().size(); i++) {
                                if (networkClientModel.getServermodel().getTable().getCards().get(i).getCost() <= networkClientModel.getServermodel().getcurrentPlayer().getCoins()) {
                                    if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Monk) {
                                        System.out.println("MONK: inserisci MONK + Colore scelto separati da uno spazio");
                                        avaiable.add("MONK");
                                    }
                                    if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Princess) {
                                        System.out.println("PRINCESS: inserisci PRINCESS + Colore scelto separati da uno spazio");
                                        avaiable.add("PRINCESS");
                                    }
                                    if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof MushroomHunter) {
                                        System.out.println("MUSHROOMHUNTER: inserisci MUSHROOMHUNTER + Colore scelto separati da uno spazio");
                                        avaiable.add("MUSHROOMHUNTER");
                                    }
                                    if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Thief) {
                                        System.out.println("THIEF: inserisci THIEF + Colore scelto separati da uno spazio");
                                        avaiable.add("THIEF");
                                    }
                                    if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Farmer) {
                                        System.out.println("FARMER: inserisci FARMER");
                                        avaiable.add("FARMER");
                                    }
                                    if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Centaur) {
                                        System.out.println("CENTAUR: inserisci CENTAUR");
                                        avaiable.add("CENTAUR");
                                    }
                                    if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Knight) {
                                        System.out.println("KNIGHT: inserisci KNIGHT");
                                        avaiable.add("KNIGHT");
                                    }
                                    if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Postman) {
                                        System.out.println("POSTMAN: inserisci POSTMAN");
                                        avaiable.add("POSTMAN");
                                    }
                                    if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Granny) {
                                        System.out.println("GRANNY: inserisci GRANNY + IndiceIsola separati da uno spazio");
                                        avaiable.add("GRANNY");
                                    }
                                    if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Herald) {
                                        System.out.println("HERALD: inserisci HERALD + IndiceIsola scelto separati da uno spazio");
                                        avaiable.add("HERALD");
                                    }
                                    if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Jester) {
                                        System.out.println("JESTER: inserisci JESTER + Inserisci i colori da scambiare da Dinner Table e poi i colori da scambiare da questa carta tutti separati da uno spazio (nota il numero di colori di uno e dell'altro deve essere lo stesso");
                                        avaiable.add("JESTER");
                                    }
                                    if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Minstrell) {
                                        System.out.println("MINSTRELL: inserisci MINSTRELL + Inserisci i colori da scambiare da Dinner Table e poi i colori da scambiare da EntranceTable tutti separati da uno spazio (nota il numero di colori di uno e dell'altro deve essere lo stesso");
                                        avaiable.add("MINSTRELL");
                                    }
                                }
                            }
                            CommandPrompt.ask("Scegliere la carta personaggio seguendo le indicazioni, un qualsiasi altra riga se si vuole tornare indietro","CHARACHTER");
                            parsedStrings =
                                    new ArrayList<String>(Arrays.asList(CommandPrompt.gotFromTerminal().split(" ")));
                            if(!avaiable.contains(parsedStrings.get(0))) {
                                requestToMe();
                                return;
                            }
                            CharacterCard modelCard=null;
                            for(int i=0;i<networkClientModel.getServermodel().getTable().getCards().size();i++){
                                if(networkClientModel.getServermodel().getTable().getCards().get(i).getName().equals(parsedStrings.get(0))){
                                    modelCard=networkClientModel.getServermodel().getTable().getCards().get(i);
                                }
                            }
                            switch (parsedStrings.get(0)) {
                                case "MUSHROOMHUNTER":
                                case "THIEF":
                                    if(!(parsedStrings.size()==2) || !(Objects.equals(parsedStrings.get(1), "RED") || Objects.equals(parsedStrings.get(1), "PINK") || Objects.equals(parsedStrings.get(1), "GREEN") || Objects.equals(parsedStrings.get(1), "YELLOW") || Objects.equals(parsedStrings.get(1), "BLUE"))) {
                                        System.out.println("formato non valido, scelta carta rifiutata !!!!");
                                        TimeUnit.SECONDS.sleep(2);
                                        requestToMe();
                                        return;
                                    }
                                    PeopleColor Color=null;
                                    switch (parsedStrings.get(1)) {
                                        case "RED":
                                            Color = PeopleColor.RED;
                                            break;
                                        case "GREEN":
                                            Color = PeopleColor.GREEN;
                                            break;
                                        case "PINK":
                                            Color = PeopleColor.PINK;
                                            break;
                                        case "BLUE":
                                            Color = PeopleColor.BLUE;
                                            break;
                                        case "YELLOW":
                                            Color = PeopleColor.YELLOW;
                                            break;
                                    }
                                    networkClientModel.setTypeOfRequest(parsedStrings.get(0));
                                    networkClientModel.setResponse(true); //lo flaggo come messaggio di risposta
                                    networkClientModel.setChoosedColor(Color);
                                    json = new Gson();
                                    isUsed=true;
                                    Network.send(json.toJson(networkClientModel));
                                break;
                                case "MONK":
                                case "PRINCESS":
                                    if(!(parsedStrings.size()==2) || !(Objects.equals(parsedStrings.get(1), "RED") || Objects.equals(parsedStrings.get(1), "PINK") || Objects.equals(parsedStrings.get(1), "GREEN") || Objects.equals(parsedStrings.get(1), "YELLOW") || Objects.equals(parsedStrings.get(1), "BLUE"))) {
                                        System.out.println("formato non valido, scelta carta rifiutata !!!!");
                                        TimeUnit.SECONDS.sleep(2);
                                        requestToMe();
                                        return;
                                    }
                                    Color=null;
                                    switch (parsedStrings.get(1)) {
                                        case "RED":
                                            Color = PeopleColor.RED;
                                            break;
                                        case "GREEN":
                                            Color = PeopleColor.GREEN;
                                            break;
                                        case "PINK":
                                            Color = PeopleColor.PINK;
                                            break;
                                        case "BLUE":
                                            Color = PeopleColor.BLUE;
                                            break;
                                        case "YELLOW":
                                            Color = PeopleColor.YELLOW;
                                            break;
                                    }
                                    if(((Monk) modelCard).getSet().numStudentsbycolor(Color)==0){
                                        System.out.println("La carta non possiede il colore che hai scelto, scelta carta rifiutata !!!!");
                                        TimeUnit.SECONDS.sleep(2);
                                        requestToMe();
                                        return;
                                    }
                                    networkClientModel.setTypeOfRequest(parsedStrings.get(0));
                                    networkClientModel.setResponse(true); //lo flaggo come messaggio di risposta
                                    networkClientModel.setChoosedColor(Color);
                                    json = new Gson();
                                    isUsed=true;
                                    Network.send(json.toJson(networkClientModel));
                                break;
                                case "FARMER":
                                case "CENTAUR":
                                case "KNIGHT":
                                case "POSTMAN":
                                    if(parsedStrings.size()!=1 ) {
                                        System.out.println("formato non valido, scelta carta rifiutata !!!!");
                                        TimeUnit.SECONDS.sleep(2);
                                        requestToMe();
                                        return;
                                    }
                                    networkClientModel.setTypeOfRequest(parsedStrings.get(0));
                                    networkClientModel.setResponse(true); //lo flaggo come messaggio di risposta
                                    json = new Gson();
                                    isUsed=true;
                                    Network.send(json.toJson(networkClientModel));
                                break;
                                case "GRANNY":
                                case "HERALD":
                                    if(!(parsedStrings.size()==2)  || (Integer.parseInt(parsedStrings.get(1))<1 || Integer.parseInt(parsedStrings.get(1))>networkClientModel.getServermodel().getTable().getIslands().size())) {
                                        System.out.println("formato non valido, scelta carta rifiutata !!!!");
                                        TimeUnit.SECONDS.sleep(2);
                                        requestToMe();
                                        return;
                                    }
                                    networkClientModel.setTypeOfRequest(parsedStrings.get(0));
                                    networkClientModel.setResponse(true); //lo flaggo come messaggio di risposta
                                    networkClientModel.setChoosedIsland(Integer.parseInt(parsedStrings.get(1)));
                                    json = new Gson();
                                    isUsed=true;
                                    Network.send(json.toJson(networkClientModel));
                                break;
                                case "MINSTRELL":
                                case "JESTER":
                                    int max=2;
                                    StudentSet destination= networkClientModel.getServermodel().getcurrentPlayer().getSchoolBoard().getEntranceSpace();
                                    ArrayList<PeopleColor> colors1= new ArrayList<>();
                                    ArrayList<PeopleColor> colors2=new ArrayList<>();
                                    if(parsedStrings.get(0).equals("JESTER")) {
                                        max = 3;
                                        destination=((Jester) modelCard).getSet();
                                    }
                                    if(parsedStrings.size()<3 || parsedStrings.size()>(1+2*max) || parsedStrings.size()%2==0) {
                                        System.out.println("formato non valido, scelta carta rifiutata !!!!");
                                        TimeUnit.SECONDS.sleep(2);
                                        requestToMe();
                                        return;
                                    }
                                    for(int i=1;i<parsedStrings.size();i++) {
                                        switch (parsedStrings.get(i)) {
                                            case "RED":
                                            if(i<=(parsedStrings.size()-1)/2) {
                                                colors1.add(PeopleColor.RED);
                                            }
                                            else {
                                                colors2.add(PeopleColor.RED);
                                            }
                                            break;
                                            case "GREEN":
                                                if(i<=(parsedStrings.size()-1)/2) {
                                                    colors1.add(PeopleColor.GREEN);
                                                }
                                                else {
                                                    colors2.add(PeopleColor.GREEN);
                                                }
                                                break;
                                            case "BLUE":
                                                if(i<=(parsedStrings.size()-1)/2) {
                                                    colors1.add(PeopleColor.BLUE);
                                                }
                                                else {
                                                    colors2.add(PeopleColor.BLUE);
                                                }
                                                break;
                                            case "YELLOW":
                                                if(i<=(parsedStrings.size()-1)/2) {
                                                    colors1.add(PeopleColor.YELLOW);
                                                }
                                                else {
                                                    colors2.add(PeopleColor.YELLOW);
                                                }
                                                break;
                                            case "PINK":
                                                if(i<=(parsedStrings.size()-1)/2) {
                                                    colors1.add(PeopleColor.PINK);
                                                }
                                                else {
                                                    colors2.add(PeopleColor.PINK);
                                                }
                                                break;
                                        }
                                    }
                                    if(!networkClientModel.getServermodel().getcurrentPlayer().getSchoolBoard().getDinnerTable().contains(colors1) || !destination.contains(colors2) ) {
                                        System.out.println("uno o piu colori scelti non sono presenti, scelta carta rifiutata !!!!");
                                        TimeUnit.SECONDS.sleep(2);
                                        requestToMe();
                                        return;
                                    }
                                    networkClientModel.setTypeOfRequest(parsedStrings.get(0));
                                    networkClientModel.setResponse(true); //lo flaggo come messaggio di risposta
                                    networkClientModel.setColors1(colors1);
                                    networkClientModel.setColors2(colors2);
                                    json = new Gson();
                                    isUsed=true;
                                    Network.send(json.toJson(networkClientModel));
                                break;
                            }
                            break;
                    }
                    //lo flaggo come messaggio di risposta
                    if (!Objects.equals(CommandPrompt.gotFromTerminal(), "CARD")) {
                        if (networkClientModel.getServermodel().getcurrentPlayer().getSchoolBoard().getEntranceSpace().numStudentsbycolor(choosedColor) == 0) {
                            System.out.println("Si è inserito un colore non presente tra quelli disponibili, reinserire i dati con più attenzione !!!!");
                            TimeUnit.SECONDS.sleep(2);
                            requestToMe();
                            return;
                        }

                        CommandPrompt.ask("Scegliere SCHOOL se si desidera " +
                                "muovere uno studente dalla tua entrance space alla sala da pranzo, " +
                                "altrimenti scrivi ISLAND se desideri muovere uno studente su un'isola, inserire una qualsiasi altra riga di comando se si vuole tornare indietro", "SCHOOL or ISLAND> ");

                        String command = CommandPrompt.gotFromTerminal();
                        if (!command.equals("SCHOOL") && !command.equals("ISLAND")) {
                            requestToMe();
                            return;
                        } else if (command.equals("SCHOOL")) {
                            if (networkClientModel.getServermodel().getcurrentPlayer().getSchoolBoard().getDinnerTable().numStudentsbycolor(choosedColor) == 10) {
                                System.out.println("La sala da pranzo di quel colore è piena.");
                                TimeUnit.SECONDS.sleep(2);
                                requestToMe();
                                return;
                            }
                            networkClientModel.setTypeOfRequest("SCHOOL");
                        } else if (command.equals("ISLAND")) {
                            CommandPrompt.ask("Inserire numero dell'isola su cui si desidera muovere lo studente", "isola> ");
                            if (!isValidNumber(CommandPrompt.gotFromTerminal())) {
                                System.out.println("Si è inserito un numero non valido, reinserire i dati con più attenzione !!!!");
                                TimeUnit.SECONDS.sleep(2);
                                requestToMe();
                                return;
                            }
                            if (Integer.parseInt(CommandPrompt.gotFromTerminal()) > networkClientModel.getServermodel().getTable().getIslands().size() ||
                                    Integer.parseInt(CommandPrompt.gotFromTerminal()) < 0) {
                                System.out.println("L'isola scelta non è valida.");
                                TimeUnit.SECONDS.sleep(2);
                                requestToMe();
                                return;
                            }
                            networkClientModel.setTypeOfRequest("ISLAND");
                            networkClientModel.setChoosedIsland(Integer.parseInt(CommandPrompt.gotFromTerminal()) - 1);
                        }
                        networkClientModel.setTypeOfRequest(command);
                    }
                }
                    else {
                            System.out.println(networkClientModel.getServermodel().toString(networkClientModel.getNickname(), "STATO DEL GIOCO: \n" + "E' IL TUO TURNO - STUDENT PHASE" + "\n\nMOSSE ALTRI GIOCATORI: " + getResponce()));

                            CommandPrompt.ask("Scegli il colore dello studente che desideri muovere ", "RED or GREEN or BLUE or YELLOW or PINK> ");

                            if (!CommandPrompt.gotFromTerminal().equals("RED") &&
                                    !CommandPrompt.gotFromTerminal().equals("GREEN") &&
                                    !CommandPrompt.gotFromTerminal().equals("BLUE") &&
                                    !CommandPrompt.gotFromTerminal().equals("YELLOW") &&
                                    !CommandPrompt.gotFromTerminal().equals("PINK")) {
                                System.out.println("Si è inserito un colore non valido, reinserire i dati con più attenzione !!!!");
                                TimeUnit.SECONDS.sleep(2);
                                requestToMe();
                                return;
                            }
                            PeopleColor choosedColor;
                            switch (CommandPrompt.gotFromTerminal()) {
                                case "RED":
                                    choosedColor = PeopleColor.RED;
                                    break;
                                case "GREEN":
                                    choosedColor = PeopleColor.GREEN;
                                    break;
                                case "BLUE":
                                    choosedColor = PeopleColor.BLUE;
                                    break;
                                case "YELLOW":
                                    choosedColor = PeopleColor.YELLOW;
                                    break;
                                case "PINK":
                                    choosedColor = PeopleColor.PINK;
                                    break;
                                default:
                                    throw new IllegalStateException("Unexpected value: " + CommandPrompt.gotFromTerminal());
                            }

                            if (networkClientModel.getServermodel().getcurrentPlayer().getSchoolBoard().getEntranceSpace().numStudentsbycolor(choosedColor) == 0) {
                                System.out.println("Si è inserito un colore non presente tra quelli disponibili, reinserire i dati con più attenzione !!!!");
                                TimeUnit.SECONDS.sleep(2);
                                requestToMe();
                                return;
                            }

                            CommandPrompt.ask("Scegliere SCHOOL se si desidera " +
                                    "muovere uno studente dalla tua entrance space alla sala da pranzo, " +
                                    "altrimenti scrivi ISLAND se desideri muovere uno studente su un'isola, inserire una qualsiasi altra riga di comando se si vuole tornare indietro", "SCHOOL or ISLAND> ");

                            String command = CommandPrompt.gotFromTerminal();
                            if (!command.equals("SCHOOL") && !command.equals("ISLAND")) {
                                requestToMe();
                                return;
                            } else if (command.equals("SCHOOL")) {
                                if (networkClientModel.getServermodel().getcurrentPlayer().getSchoolBoard().getDinnerTable().numStudentsbycolor(choosedColor) == 10) {
                                    System.out.println("La sala da pranzo di quel colore è piena.");
                                    TimeUnit.SECONDS.sleep(2);
                                    requestToMe();
                                    return;
                                }
                                networkClientModel.setTypeOfRequest("SCHOOL");
                            } else if (command.equals("ISLAND")) {
                                CommandPrompt.ask("Inserire numero dell'isola su cui si desidera muovere lo studente", "isola> ");
                                if (!isValidNumber(CommandPrompt.gotFromTerminal())) {
                                    System.out.println("Si è inserito un numero non valido, reinserire i dati con più attenzione !!!!");
                                    TimeUnit.SECONDS.sleep(2);
                                    requestToMe();
                                    return;
                                }
                                if (Integer.parseInt(CommandPrompt.gotFromTerminal()) > networkClientModel.getServermodel().getTable().getIslands().size() ||
                                        Integer.parseInt(CommandPrompt.gotFromTerminal()) < 0) {
                                    System.out.println("L'isola scelta non è valida.");
                                    TimeUnit.SECONDS.sleep(2);
                                    requestToMe();
                                    return;
                                }
                                networkClientModel.setTypeOfRequest("ISLAND");
                                networkClientModel.setChoosedIsland(Integer.parseInt(CommandPrompt.gotFromTerminal()) - 1);
                            }
                    networkClientModel.setTypeOfRequest(command);
                    networkClientModel.setResponse(true); //lo flaggo come messaggio di risposta
                    networkClientModel.setChoosedColor(choosedColor);
                    json = new Gson();
                    Network.send(json.toJson(networkClientModel));
                    break;
                    }
                break;
            case "TEAMMATE" :
                for (String nickname : networkClientModel.getNicknames()){
                    System.out.println(nickname);
                }
                CommandPrompt.ask("Inserisci il nickname del tuo compagno di squadra: ","Nickname> ");
                if(!networkClientModel.getNicknames().contains(CommandPrompt.gotFromTerminal())){
                    System.out.print("Il nickname inserito non esiste, si prega di scegliere un nickname tra quelli specificati");
                    requestToMe();
                    return;
                }
                String teamMate = CommandPrompt.gotFromTerminal();
                networkClientModel.getNicknames().remove(teamMate);
                networkClientModel.getNicknames().add(teamMate);
                networkClientModel.getNicknames().add(networkClientModel.getNickname());
                networkClientModel.setResponse(true); //lo flaggo come messaggio di risposta
                networkClientModel.setFromTerminal(parsedStrings);
                json = new Gson();
                Network.send(json.toJson(networkClientModel));
                break;

            case "CHOOSEWHERETOMOVEMOTHER" :
                if(networkClientModel.getServermodel().getGameMode().equals(GameMode.EXPERT) && networkClientModel.getServermodel().getTable().getCards().stream().anyMatch(j->j.getCost()<= networkClientModel.getServermodel().getcurrentPlayer().getCoins() && !isUsed)) {
                    System.out.println(networkClientModel.getServermodel().toString(networkClientModel.getNickname(), "STATO DEL GIOCO:\n " + "E' IL TUO TURNO - MOTHER PHASE" + "\n\nMOSSE ALTRI GIOCATORI: " + getResponce()));
                    CommandPrompt.ask("Scegliere il numero di mosse di cui far spostare madre natura  OPPURE inserisci CARD per usare una carta personaggio", "mosse> ");
                    if (CommandPrompt.gotFromTerminal().equals("CARD")) {
                        System.out.println("Scegli la carta che vorresti utilizzare: \n");
                        ArrayList<String> avaiable = new ArrayList<>();
                        for (int i = 0; i < networkClientModel.getServermodel().getTable().getCards().size(); i++) {
                            if (networkClientModel.getServermodel().getTable().getCards().get(i).getCost() <= networkClientModel.getServermodel().getcurrentPlayer().getCoins()) {
                                if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Monk) {
                                    System.out.println("MONK: inserisci MONK + Colore scelto separati da uno spazio");
                                    avaiable.add("MONK");
                                }
                                if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Princess) {
                                    System.out.println("PRINCESS: inserisci PRINCESS + Colore scelto separati da uno spazio");
                                    avaiable.add("PRINCESS");
                                }
                                if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof MushroomHunter) {
                                    System.out.println("MUSHROOMHUNTER: inserisci MUSHROOMHUNTER + Colore scelto separati da uno spazio");
                                    avaiable.add("MUSHROOMHUNTER");
                                }
                                if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Thief) {
                                    System.out.println("THIEF: inserisci THIEF + Colore scelto separati da uno spazio");
                                    avaiable.add("THIEF");
                                }
                                if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Farmer) {
                                    System.out.println("FARMER: inserisci FARMER");
                                    avaiable.add("FARMER");
                                }
                                if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Centaur) {
                                    System.out.println("CENTAUR: inserisci CENTAUR");
                                    avaiable.add("CENTAUR");
                                }
                                if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Knight) {
                                    System.out.println("KNIGHT: inserisci KNIGHT");
                                    avaiable.add("KNIGHT");
                                }
                                if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Postman) {
                                    System.out.println("POSTMAN: inserisci POSTMAN");
                                    avaiable.add("POSTMAN");
                                }
                                if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Granny) {
                                    System.out.println("GRANNY: inserisci GRANNY + IndiceIsola separati da uno spazio");
                                    avaiable.add("GRANNY");
                                }
                                if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Herald) {
                                    System.out.println("HERALD: inserisci HERALD + IndiceIsola scelto separati da uno spazio");
                                    avaiable.add("HERALD");
                                }
                                if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Jester) {
                                    System.out.println("JESTER: inserisci JESTER + Inserisci i colori da scambiare da Dinner Table e poi i colori da scambiare da questa carta tutti separati da uno spazio (nota il numero di colori di uno e dell'altro deve essere lo stesso");
                                    avaiable.add("JESTER");
                                }
                                if (networkClientModel.getServermodel().getTable().getCards().get(i) instanceof Minstrell) {
                                    System.out.println("MINSTRELL: inserisci MINSTRELL + Inserisci i colori da scambiare da Dinner Table e poi i colori da scambiare da EntranceTable tutti separati da uno spazio (nota il numero di colori di uno e dell'altro deve essere lo stesso");
                                    avaiable.add("MINSTRELL");
                                }
                            }
                        }
                        CommandPrompt.ask("Scegliere la carta personaggio seguendo le indicazioni, un qualsiasi altra riga se si vuole tornare indietro","CHARACHTER");
                        parsedStrings =
                                new ArrayList<String>(Arrays.asList(CommandPrompt.gotFromTerminal().split(" ")));
                        if(!avaiable.contains(parsedStrings.get(0))) {
                            requestToMe();
                            return;
                        }
                        CharacterCard modelCard=null;
                        for(int i=0;i<networkClientModel.getServermodel().getTable().getCards().size();i++){
                            if(networkClientModel.getServermodel().getTable().getCards().get(i).getName().equals(parsedStrings.get(0))){
                                modelCard=networkClientModel.getServermodel().getTable().getCards().get(i);
                            }
                        }
                        switch (parsedStrings.get(0)) {
                            case "MUSHROOMHUNTER":
                            case "THIEF":
                                if(!(parsedStrings.size()==2) || !(Objects.equals(parsedStrings.get(1), "RED") || Objects.equals(parsedStrings.get(1), "PINK") || Objects.equals(parsedStrings.get(1), "GREEN") || Objects.equals(parsedStrings.get(1), "YELLOW") || Objects.equals(parsedStrings.get(1), "BLUE"))) {
                                    System.out.println("formato non valido, scelta carta rifiutata !!!!");
                                    TimeUnit.SECONDS.sleep(2);
                                    requestToMe();
                                    return;
                                }
                                PeopleColor Color=null;
                                switch (parsedStrings.get(1)) {
                                    case "RED":
                                        Color = PeopleColor.RED;
                                        break;
                                    case "GREEN":
                                        Color = PeopleColor.GREEN;
                                        break;
                                    case "PINK":
                                        Color = PeopleColor.PINK;
                                        break;
                                    case "BLUE":
                                        Color = PeopleColor.BLUE;
                                        break;
                                    case "YELLOW":
                                        Color = PeopleColor.YELLOW;
                                        break;
                                }
                                networkClientModel.setTypeOfRequest(parsedStrings.get(0));
                                networkClientModel.setResponse(true); //lo flaggo come messaggio di risposta
                                networkClientModel.setChoosedColor(Color);
                                json = new Gson();
                                isUsed=true;
                                Network.send(json.toJson(networkClientModel));
                                break;
                            case "MONK":
                            case "PRINCESS":
                                if(!(parsedStrings.size()==2) || !(Objects.equals(parsedStrings.get(1), "RED") || Objects.equals(parsedStrings.get(1), "PINK") || Objects.equals(parsedStrings.get(1), "GREEN") || Objects.equals(parsedStrings.get(1), "YELLOW") || Objects.equals(parsedStrings.get(1), "BLUE"))) {
                                    System.out.println("formato non valido, scelta carta rifiutata !!!!");
                                    TimeUnit.SECONDS.sleep(2);
                                    requestToMe();
                                    return;
                                }
                                Color=null;
                                switch (parsedStrings.get(1)) {
                                    case "RED":
                                        Color = PeopleColor.RED;
                                        break;
                                    case "GREEN":
                                        Color = PeopleColor.GREEN;
                                        break;
                                    case "PINK":
                                        Color = PeopleColor.PINK;
                                        break;
                                    case "BLUE":
                                        Color = PeopleColor.BLUE;
                                        break;
                                    case "YELLOW":
                                        Color = PeopleColor.YELLOW;
                                        break;
                                }
                                if(((Monk) modelCard).getSet().numStudentsbycolor(Color)==0){
                                    System.out.println("La carta non possiede il colore che hai scelto, scelta carta rifiutata !!!!");
                                    TimeUnit.SECONDS.sleep(2);
                                    requestToMe();
                                    return;
                                }
                                networkClientModel.setTypeOfRequest(parsedStrings.get(0));
                                networkClientModel.setResponse(true); //lo flaggo come messaggio di risposta
                                networkClientModel.setChoosedColor(Color);
                                json = new Gson();
                                isUsed=true;
                                Network.send(json.toJson(networkClientModel));
                                break;
                            case "FARMER":
                            case "CENTAUR":
                            case "KNIGHT":
                            case "POSTMAN":
                                if(parsedStrings.size()!=1 ) {
                                    System.out.println("formato non valido, scelta carta rifiutata !!!!");
                                    TimeUnit.SECONDS.sleep(2);
                                    requestToMe();
                                    return;
                                }
                                networkClientModel.setTypeOfRequest(parsedStrings.get(0));
                                networkClientModel.setResponse(true); //lo flaggo come messaggio di risposta
                                json = new Gson();
                                isUsed=true;
                                Network.send(json.toJson(networkClientModel));
                                break;
                            case "GRANNY":
                            case "HERALD":
                                if(!(parsedStrings.size()==2)  || (Integer.parseInt(parsedStrings.get(1))<1 || Integer.parseInt(parsedStrings.get(1))>networkClientModel.getServermodel().getTable().getIslands().size())) {
                                    System.out.println("formato non valido, scelta carta rifiutata !!!!");
                                    TimeUnit.SECONDS.sleep(2);
                                    requestToMe();
                                    return;
                                }
                                networkClientModel.setTypeOfRequest(parsedStrings.get(0));
                                networkClientModel.setResponse(true); //lo flaggo come messaggio di risposta
                                networkClientModel.setChoosedIsland(Integer.parseInt(parsedStrings.get(1)));
                                json = new Gson();
                                isUsed=true;
                                Network.send(json.toJson(networkClientModel));
                                break;
                            case "MINSTRELL":
                            case "JESTER":
                                int max=2;
                                StudentSet destination= networkClientModel.getServermodel().getcurrentPlayer().getSchoolBoard().getEntranceSpace();
                                ArrayList<PeopleColor> colors1= new ArrayList<>();
                                ArrayList<PeopleColor> colors2=new ArrayList<>();
                                if(parsedStrings.get(0).equals("JESTER")) {
                                    max = 3;
                                    destination=((Jester) modelCard).getSet();
                                }
                                if(parsedStrings.size()<3 || parsedStrings.size()>(1+2*max) || parsedStrings.size()%2==0) {
                                    System.out.println("formato non valido, scelta carta rifiutata !!!!");
                                    TimeUnit.SECONDS.sleep(2);
                                    requestToMe();
                                    return;
                                }
                                for(int i=1;i<parsedStrings.size();i++) {
                                    switch (parsedStrings.get(i)) {
                                        case "RED":
                                            if(i<=(parsedStrings.size()-1)/2) {
                                                colors1.add(PeopleColor.RED);
                                            }
                                            else {
                                                colors2.add(PeopleColor.RED);
                                            }
                                            break;
                                        case "GREEN":
                                            if(i<=(parsedStrings.size()-1)/2) {
                                                colors1.add(PeopleColor.GREEN);
                                            }
                                            else {
                                                colors2.add(PeopleColor.GREEN);
                                            }
                                            break;
                                        case "BLUE":
                                            if(i<=(parsedStrings.size()-1)/2) {
                                                colors1.add(PeopleColor.BLUE);
                                            }
                                            else {
                                                colors2.add(PeopleColor.BLUE);
                                            }
                                            break;
                                        case "YELLOW":
                                            if(i<=(parsedStrings.size()-1)/2) {
                                                colors1.add(PeopleColor.YELLOW);
                                            }
                                            else {
                                                colors2.add(PeopleColor.YELLOW);
                                            }
                                            break;
                                        case "PINK":
                                            if(i<=(parsedStrings.size()-1)/2) {
                                                colors1.add(PeopleColor.PINK);
                                            }
                                            else {
                                                colors2.add(PeopleColor.PINK);
                                            }
                                            break;
                                    }
                                }
                                if(!networkClientModel.getServermodel().getcurrentPlayer().getSchoolBoard().getDinnerTable().contains(colors1) || !destination.contains(colors2) ) {
                                    System.out.println("uno o piu colori scelti non sono presenti, scelta carta rifiutata !!!!");
                                    TimeUnit.SECONDS.sleep(2);
                                    requestToMe();
                                    return;
                                }
                                networkClientModel.setTypeOfRequest(parsedStrings.get(0));
                                networkClientModel.setResponse(true); //lo flaggo come messaggio di risposta
                                networkClientModel.setColors1(colors1);
                                networkClientModel.setColors2(colors2);
                                json = new Gson();
                                isUsed=true;
                                Network.send(json.toJson(networkClientModel));
                                break;
                        }
                        break;

                    } else {
                        if (!isValidNumber(CommandPrompt.gotFromTerminal())) {
                            System.out.println("Il numero di mosse da te inserito non è un numero valido, si prega di fare più attenzione");
                            TimeUnit.SECONDS.sleep(2);
                            requestToMe();
                            return;
                        }
                        if (Integer.parseInt((CommandPrompt.gotFromTerminal())) <= 0) {
                            System.out.println("Il numero di mosse deve essere un numero intero positivo, fare più attenzione");
                            TimeUnit.SECONDS.sleep(2);
                            requestToMe();
                            break;
                        }
                        if (networkClientModel.getServermodel().getcurrentPlayer().getChoosedCard().getMoves() < Integer.parseInt(CommandPrompt.gotFromTerminal())) {
                            System.out.println("Il numero di mosse da te inserito eccede il numero massimo di mosse di cui madre natura può spostarsi," +
                                    " ovvero " + networkClientModel.getServermodel().getcurrentPlayer().getChoosedCard().getMoves());
                            TimeUnit.SECONDS.sleep(2);
                            requestToMe();
                            return;
                        }
                        networkClientModel.setTypeOfRequest("MOTHER");
                        networkClientModel.setChoosedMoves(Integer.parseInt(CommandPrompt.gotFromTerminal()));
                        networkClientModel.setResponse(true); //lo flaggo come messaggio di risposta
                        networkClientModel.setFromTerminal(parsedStrings);
                        json = new Gson();
                        Network.send(json.toJson(networkClientModel));
                    }
                }
                else {
                    System.out.println(networkClientModel.getServermodel().toString(networkClientModel.getNickname(), "STATO DEL GIOCO:\n " + "E' IL TUO TURNO - MOTHER PHASE" + "\n\nMOSSE ALTRI GIOCATORI: " + getResponce()));
                    CommandPrompt.ask("Scegliere il numero di mosse di cui far spostare madre natura  ", "mosse> ");

                    if (!isValidNumber(CommandPrompt.gotFromTerminal())) {
                        System.out.println("Il numero di mosse da te inserito non è un numero valido, si prega di fare più attenzione");
                        TimeUnit.SECONDS.sleep(2);
                        requestToMe();
                        return;
                    }
                    if (Integer.parseInt((CommandPrompt.gotFromTerminal())) <= 0) {
                        System.out.println("Il numero di mosse deve essere un numero intero positivo, fare più attenzione");
                        TimeUnit.SECONDS.sleep(2);
                        requestToMe();
                        break;
                    }
                    if (networkClientModel.getServermodel().getcurrentPlayer().getChoosedCard().getMoves() < Integer.parseInt(CommandPrompt.gotFromTerminal())) {
                        System.out.println("Il numero di mosse da te inserito eccede il numero massimo di mosse di cui madre natura può spostarsi," +
                                " ovvero " + networkClientModel.getServermodel().getcurrentPlayer().getChoosedCard().getMoves());
                        TimeUnit.SECONDS.sleep(2);
                        requestToMe();
                        return;
                    }
                    networkClientModel.setTypeOfRequest("MOTHER");
                    networkClientModel.setChoosedMoves(Integer.parseInt(CommandPrompt.gotFromTerminal()));
                    networkClientModel.setResponse(true); //lo flaggo come messaggio di risposta
                    networkClientModel.setFromTerminal(parsedStrings);
                    json = new Gson();
                    Network.send(json.toJson(networkClientModel));
                }

                break;

            case "CHOOSECLOUDS" :
                System.out.println(networkClientModel.getServermodel().toString(networkClientModel.getNickname(),"STATO DEL GIOCO: \n"+"E' IL TUO TURNO - CLOUD PHASE"+ "\n\nMOSSE ALTRI GIOCATORI: "+getResponce()));
                CommandPrompt.ask("Scegliere il numero della tessera nuvola da cui si desidera ricaricarsi di studenti","tessera nuvola> ");

                if(!isValidNumber(CommandPrompt.gotFromTerminal())){
                    System.out.println("Il numero inserito non è un numero valido");
                    TimeUnit.SECONDS.sleep(2);
                    requestToMe();
                    return;
                }
                if( networkClientModel.getServermodel().getTable().getClouds().size() < Integer.parseInt(CommandPrompt.gotFromTerminal()) ||
                    Integer.parseInt(CommandPrompt.gotFromTerminal()) < 1 ){
                    System.out.println("Il numero inserito non rappresenta una tessera nuvola esistente, si prega di fare più attenzione");
                    TimeUnit.SECONDS.sleep(2);
                    requestToMe();
                    return;
                }
                // la nuvola scelta deve avere una size > 0
                if(networkClientModel.getServermodel().getTable().getClouds().get(Integer.parseInt(CommandPrompt.gotFromTerminal()) - 1).getStudentsAccumulator().size() == 0){
                    System.out.println("Hai scelta una nuvola che è stata già scelta da un altro giocatore");
                    TimeUnit.SECONDS.sleep(2);
                    requestToMe();
                    return;
                }
                networkClientModel.setCloudChoosed(networkClientModel.getServermodel().getTable().getClouds().get(Integer.parseInt(CommandPrompt.gotFromTerminal()) - 1));
                networkClientModel.setResponse(true); //lo flaggo come messaggio di risposta
                networkClientModel.setFromTerminal(parsedStrings);
                json = new Gson();
                Network.send(json.toJson(networkClientModel));
                break;

            case "GAMEEND":
                System.out.println("Il vincitore/i è/sono :" + networkClientModel.getGameWinner());
                Network.disconnect();
            break;

        }
        clearResponce();

    }

    // Qualcun altro sta interagendo con il terminale: devo gestire il tempo di attesa
    // Esempio "pippo: sta salutando"
    public void requestToOthers() throws IOException {
        String message=null;
        switch(networkClientModel.getTypeOfRequest()) {
            case "CHOOSEASSISTANTCARD" :
                message="L'utente " +networkClientModel.getNickname()+ " sta scegliendo la carta assistente";
                break;
            case "CHOOSEWHERETOMOVESTUDENTS"    :
                message="L'utente " +networkClientModel.getNickname()+ " sta scegliendo dove muovere lo studente";
                break;
            case "TEAMMATE" :
                message="L'utente " +networkClientModel.getNickname()+ " sta scegliendo il suo compagno di squadra";
                break;
            case "CHOOSEWHERETOMOVEMOTHER"    :
                message="L'utente " +networkClientModel.getNickname()+ " sta scegliendo il numero di mosse di cui far spostare madre natura";
                break;
            case "CHOOSECLOUDS"    :
                message="L'utente " +networkClientModel.getNickname()+ " sta scegliendo la nuvola dalla quale ricaricare gli studenti";
                break;
        }
        System.out.println(networkClientModel.getServermodel().toString(getMynickname(),"STATO DEL GIOCO: "+message+ "\n\nMOSSE ALTRI GIOCATORI: "+getResponce()));
    }

    // Qualcun altro ha risposto al server: devo mostrare a schermo un'interpretazione della risposta
    // Esempio "pippo: ha salutato"
    public void response() throws IOException {
        switch(networkClientModel.getTypeOfRequest()) {
            case "CHOOSEASSISTANTCARD":
                setResponce("Il giocatore " + networkClientModel.getNickname()+" ha scelto " +
                        "la carta col valore = " +networkClientModel.getCardChoosedValue());
                break;
            case "TEAMMATE" :
                setResponce("Il giocatore " + networkClientModel.getNickname()+" ha formato i teams:\n" +
                        "TEAM 1: " + networkClientModel.getNicknames().get(3) + " " + networkClientModel.getNicknames().get(2)+"\n" +
                        "TEAM 2: " + networkClientModel.getNicknames().get(1) + " " + networkClientModel.getNicknames().get(0)+"\n" );
                break;
            case "SCHOOL"    :
                setResponce("L'utente " +networkClientModel.getNickname()+ " ha scelto di muovere 1 studente di colore " +
                         networkClientModel.getChoosedColor().toString() +" sulla sua scuola");
                break;
            case "ISLAND"    :
                setResponce("L'utente " +networkClientModel.getNickname()+ " ha scelto di muovere 1 studente di colore " +
                        networkClientModel.getChoosedColor().toString() +" sull' isola numero " +(networkClientModel.getChoosedIsland()+1));
                break;
            case "CHOOSEWHERETOMOVEMOTHER"    :
                setResponce("L'utente " +networkClientModel.getNickname()+ " ha scelto di spostare madre natura di " + networkClientModel.getChoosedMoves() + " mosse");
                break;
            case "CHOOSECLOUDS":
                setResponce("L'utente " +networkClientModel.getNickname()+ " ha scelto di ricaricare gli studenti dalla nuvola: " + networkClientModel.getCloudChoosed());
        }
    }





    // Methods for client side type-safety checking
    // Metodo che controlla se un IP è valido con le regular expressions


    // Metodo che controlla se una porta è valida con le regular expressions

    // Metodo che controlla se una cifra è valida con le regular expressions
    public static boolean isValidCifra(String cifra)
    {

        String CIFRA_REGEX ="[0-9]";

        Pattern CIFRA_PATTERN = Pattern.compile(CIFRA_REGEX);

        if (cifra == null) {
            return false;
        }

        Matcher matcher = CIFRA_PATTERN.matcher(cifra);

        return matcher.matches();
    }

    // Metodo che controlla se un numero è valido con le regular expressions
    public static boolean isValidNumber(String number)
    {
        String NUMERO_REGEX ="[0-9]+";
        Pattern NUMERO_PATTERN = Pattern.compile(NUMERO_REGEX);

        if (number == null) {
            return false;
        }

        Matcher matcher = NUMERO_PATTERN.matcher(number);

        return matcher.matches();
    }

    public String getMynickname() {
        return mynickname;
    }

    public void setMynickname(String mynickname) {
        this.mynickname = mynickname;
    }

    public String getResponce() {
        return responce;
    }

    public void setResponce(String responce) {
        this.responce = this.responce+"\n"+responce;
    }
    public void clearResponce(){
        this.responce="\n";
    }
}

