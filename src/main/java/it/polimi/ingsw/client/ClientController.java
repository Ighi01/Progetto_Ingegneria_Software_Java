package it.polimi.ingsw.client;

import it.polimi.ingsw.client.states.*;
import it.polimi.ingsw.client.view.*;
import it.polimi.ingsw.server.states.Idle;
import it.polimi.ingsw.utils.cli.CommandPrompt;
import it.polimi.ingsw.utils.stateMachine.*;
import java.io.IOException;

public class ClientController {

    public ClientController(View view) throws IOException, InterruptedException {

        CommandPrompt.setDebug();
        // modello dati di questa semplice demo
        ClientModel clientModel = new ClientModel();
        // vista (per il momento solo command line interface)

        // Dichiarazione degli stati necessari
        // La maggior parte delle volte uno stato rappresenta una schermata (che sia essa di gui o di cli)
        // ma più in generale potrebbe significare delle azioni da compiere a fronte di eventi
        // modo elegante di far partire il controllore
        Idle idle = new Idle();
        Event start = new Event("[Controller Started]");
        Controller fsm = new Controller("Controllore del Client", idle);

        // Costruzioni degli stati necessari

        // La welcomeScreen richiede all'utente una interazione scrivendo (o cliccando il bottone) start
        WelcomeScreen waitStart = new WelcomeScreen(view);

        // Stato di connessione al server
        ConnectToServer connectionToServer = new ConnectToServer(view, clientModel);

        // Chiede al server se è il primo client ad essersi collegato
        AmIFirst amIFirst = new AmIFirst(clientModel,fsm);

        // Reading stuffs from terminal
        ReadUserInfo askUserinfo = new ReadUserInfo(view, clientModel, fsm);
        ReadGameCode askGameCode = new ReadGameCode(view, clientModel, fsm);
        ReadGameInfo askGameInfo = new ReadGameInfo(view, clientModel, fsm);
        ReadWichCard askCardChoosed = new ReadWichCard(view, clientModel, fsm);
        ReadWichStudent askwitchStudent = new ReadWichStudent(view, clientModel, fsm);
        ReadWichIsland askwitchIsland = new ReadWichIsland(view, clientModel, fsm);
        ReadWhereToMoveMotherNature askwheremovemother = new ReadWhereToMoveMotherNature(view, clientModel, fsm);
        ReadWichCloud askwitchcloud = new ReadWichCloud(view, clientModel, fsm);

        CreateOrConnect connectOrCreate = new CreateOrConnect(view, clientModel);
        Decision islandOrSchool = new IslandsOrSchool(view, clientModel);

        Send_CreationParameters sendGameInfo = new Send_CreationParameters(view, clientModel, "CREATIONPARAMETERS");
        Send_CardChoosed sendCardChoosed = new Send_CardChoosed(view, clientModel, "CARDCHOOSED");
        Send_StudentToSchool sendStudent_toSchool = new Send_StudentToSchool(view, clientModel, "STUDENT_TOSCHOOL");
        Send_StudentToIsland sendStudent_toIsland = new Send_StudentToIsland(view, clientModel, "STUDENT_TOISLAND");
        Send_MotherMovement sendMotherMovement = new Send_MotherMovement(view, clientModel, "MOTHER_TOISLAND");
        Send_to_CloudChoosed sendcloudChoosed = new Send_to_CloudChoosed(view, clientModel, "CLOUDCHOOSED");
        Send_ConnectGame send_connectGame=new Send_ConnectGame(view, clientModel,"CONNECTTOGAME");

        WaitForStartGame waitstartgame = new WaitForStartGame(view, clientModel, "GAMESTART");
        WaitForTurn waitAssistantCardphase = new WaitForTurn(view, clientModel,"ASSISTANTCARDPHASE");
        WaitForTurn waitStudent_and_Mother_Phase = new WaitForTurn(view, clientModel,"STUDENTPHASE_OR_MOVEMOTHERPHASE");
        WaitForTurn waitCloudPhase = new WaitForTurn(view, clientModel,"CLOUDPHASE");
        EndGame end = new EndGame(view, clientModel);

        // Dichiarazione delle transizioni tra gli stati
        fsm.addTransition(idle, start, waitStart);

        // Schermata di benvenuto
        fsm.addTransition(waitStart, waitStart.start(), askUserinfo);
        fsm.addTransition(waitStart, waitStart.notStart(), waitStart);

        // Schermata di richiesta di nickname, ip e porta + Trying to connect to server via TCP socket
        fsm.addTransition(askUserinfo, askUserinfo.insertedParameters(), connectionToServer);
        fsm.addTransition(askUserinfo, askUserinfo.numberOfParametersIncorrect(), askUserinfo);

        // Tentativo di connessione al server sull' IP/Porta specificata dall'utente
        fsm.addTransition(connectionToServer, connectionToServer.connectionToServerFailed(), askUserinfo);
        fsm.addTransition(connectionToServer, connectionToServer.connectedToServer(), amIFirst);

        // Se sono il primo client allora devo chiedere la scelta del gamemode e del numero dei partecipanti
        // ed inviare il tutto al server

        fsm.addTransition(amIFirst, amIFirst.yes(), askGameInfo);
        /*
        // Choose if create a new game or connect to an existing one
        fsm.addTransition(connectOrCreate, connectOrCreate.haScelto2(), askGameCode);    //CONNECT
        fsm.addTransition(askGameCode, askGameCode.insertedParameters(), send_connectGame);
        fsm.addTransition(askGameCode, askGameCode.numberOfParametersIncorrect(), askGameCode);
        fsm.addTransition(send_connectGame, send_connectGame.Recevied_ack(), waitstartgame);
        fsm.addTransition(send_connectGame, send_connectGame.send_failed(), send_connectGame);
        fsm.addTransition(send_connectGame, send_connectGame.Message_not_valid(), send_connectGame);

        fsm.addTransition(connectOrCreate, connectOrCreate.haScelto1(), askGameInfo);    //CREATE THEN CONNECT
        fsm.addTransition(askGameInfo, askGameInfo.insertedParameters(), sendGameInfo);
        fsm.addTransition(askGameInfo, askGameInfo.numberOfParametersIncorrect(), askGameInfo);
        fsm.addTransition(sendGameInfo, sendGameInfo.Recevied_ack(), send_connectGame);
        fsm.addTransition(sendGameInfo, sendGameInfo.send_failed(), sendGameInfo);
        fsm.addTransition(sendGameInfo, sendGameInfo.Message_not_valid(), askGameInfo);

        fsm.addTransition(connectOrCreate, connectOrCreate.sceltaNonValida(), connectOrCreate);

        fsm.addTransition(waitstartgame, waitstartgame.start_game(), waitAssistantCardphase); //WAIT for GAMESTART from server

        //GAME
        //Thread background_thread= new Thread(new receive_view_from_server(view));
        //background_thread.start();

        //WAIT for DOASSISTANTCARDPHASE from server
        fsm.addTransition(waitAssistantCardphase, waitAssistantCardphase.go_to_assistantcardphase(), askCardChoosed);

        //ASSISTANTCARD PHASE
        fsm.addTransition(askCardChoosed, askCardChoosed.insertedParameters(), sendCardChoosed);
        fsm.addTransition(askCardChoosed, askCardChoosed.numberOfParametersIncorrect(), askCardChoosed);
        fsm.addTransition(sendCardChoosed, sendCardChoosed.Recevied_ack(), waitStudent_and_Mother_Phase);
        fsm.addTransition(sendCardChoosed, sendCardChoosed.send_failed(), sendCardChoosed);
        fsm.addTransition(sendCardChoosed, sendCardChoosed.Message_not_valid(), askCardChoosed);

        //WAIT for DOSTUDENTPHASE or DOMOTHERPHASE from server
        fsm.addTransition(waitStudent_and_Mother_Phase, waitStudent_and_Mother_Phase.go_to_studentphase() , askwitchStudent);
        fsm.addTransition(waitStudent_and_Mother_Phase, waitStudent_and_Mother_Phase.go_to_motherphase() , askwheremovemother);

        //STUDENT AND MOTHER PHASE
        fsm.addTransition(askwitchStudent, askwitchStudent.insertedParameters(), islandOrSchool);
        fsm.addTransition(askwitchStudent, askwitchStudent.numberOfParametersIncorrect(), askwitchStudent);
        fsm.addTransition(islandOrSchool, islandOrSchool.haScelto1(), askwitchIsland);
        fsm.addTransition(islandOrSchool, islandOrSchool.haScelto2(), sendStudent_toSchool);
        fsm.addTransition(islandOrSchool, islandOrSchool.sceltaNonValida(), islandOrSchool);
        fsm.addTransition(askwitchIsland, askwitchIsland.insertedParameters(), sendStudent_toIsland);
        fsm.addTransition(askwitchIsland, askwitchIsland.numberOfParametersIncorrect(), askwitchIsland);

        fsm.addTransition(sendStudent_toSchool, sendStudent_toSchool.Recevied_ack(), waitStudent_and_Mother_Phase);
        fsm.addTransition(sendStudent_toSchool, sendStudent_toSchool.send_failed(), sendStudent_toSchool);
        fsm.addTransition(sendStudent_toSchool, sendStudent_toSchool.Message_not_valid(), askwitchStudent);
        fsm.addTransition(sendStudent_toIsland, sendStudent_toIsland.Recevied_ack(), waitStudent_and_Mother_Phase);
        fsm.addTransition(sendStudent_toIsland, sendStudent_toIsland.send_failed(), sendStudent_toIsland);
        fsm.addTransition(sendStudent_toIsland, sendStudent_toIsland.Message_not_valid(), askwitchStudent);

        fsm.addTransition(askwheremovemother, askwheremovemother.insertedParameters(), sendMotherMovement);
        fsm.addTransition(askwheremovemother, askwheremovemother.numberOfParametersIncorrect(), askwheremovemother);
        fsm.addTransition(sendMotherMovement, sendMotherMovement.Recevied_ack(), waitCloudPhase);
        fsm.addTransition(sendMotherMovement, sendMotherMovement.send_failed(), sendMotherMovement);
        fsm.addTransition(sendMotherMovement, sendMotherMovement.Message_not_valid(), askwheremovemother);

        //WAIT for DOCLOUDPHASE from server
        fsm.addTransition(waitCloudPhase, waitCloudPhase.go_to_cloudphase(), askwitchcloud);

        //CLOUDPHASE
        fsm.addTransition(askwitchcloud, askwitchcloud.insertedParameters(), sendcloudChoosed);
        fsm.addTransition(askwitchcloud, askwitchcloud.numberOfParametersIncorrect(), askwitchcloud);
        fsm.addTransition(sendcloudChoosed, sendcloudChoosed.Recevied_ack(), waitAssistantCardphase);
        fsm.addTransition(sendcloudChoosed, sendcloudChoosed.send_failed(), sendcloudChoosed);
        fsm.addTransition(sendcloudChoosed, sendcloudChoosed.Message_not_valid(), askwitchcloud);

        //ENDGAME : if in the wait state's it received GOENDGAME from server
        fsm.addTransition(waitCloudPhase, waitCloudPhase.go_to_endgame(), end);
        fsm.addTransition(waitAssistantCardphase, waitAssistantCardphase.go_to_endgame(), end);
        fsm.addTransition(waitStudent_and_Mother_Phase, waitStudent_and_Mother_Phase.go_to_endgame() , end);
        */
        // L'evento di start è l'unico che deve essere fatto partire manualmente
        start.fireStateEvent();
    }

}
