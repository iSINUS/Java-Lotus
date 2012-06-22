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
      Session session = NotesFactory.createSession(args[0], args[1], args[2]);
      // Operational code goes here
      
      String server = session.getServerName();
      System.out.println("Server: " + server);
      
      Database db = session.getDatabase(server, args[3]);
      System.out.println("Database: "+db.getTitle());

      View view = db.getView("All Documents");
      Document doc = view.getFirstDocument();
      System.out.println("Document: "+doc.getHttpURL());
      
      session.recycle();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
