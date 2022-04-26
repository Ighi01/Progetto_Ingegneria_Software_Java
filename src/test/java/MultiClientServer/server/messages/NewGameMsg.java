package MultiClientServer.server.messages;

import MultiClientServer.server.ClientHandler;

import java.io.IOException;


/**
 * A message sent to request a new game to the server.
 */
public class NewGameMsg extends CommandMsg
{
  @Override
  public void processMessage(ClientHandler clientHandler) throws IOException
  {
    clientHandler.getGame().newGame();
  }
}
