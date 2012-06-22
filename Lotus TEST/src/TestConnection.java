import lotus.domino.*;

public class TestConnection
{

  /**
   * @param args
   */
  public static void main(String[] args)
  {
    // TODO Auto-generated function stub
    try
    {
      Session s = NotesFactory.createSession(args[0], args[1], args[2]);
      // Operational code goes here
      
      String server = s.getServerName();
      System.out.println(server);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
