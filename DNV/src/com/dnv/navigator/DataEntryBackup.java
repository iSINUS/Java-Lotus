package com.dnv.navigator;

import java.io.*;
import java.util.zip.*;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import lotus.domino.*;
import lotus.domino.ws.utils.FileUtils;

public class DataEntryBackup {	
	private String backupType;
	private String backupPath;
	private String backupName;
	private String backupVersion;
	Document profileDoc;
	Document template;
	
	public DataEntryBackup(Session session) throws NotesException {		
		Database database = session.getCurrentDatabase();
		this.profileDoc = database.getView("(Settings)").getFirstDocument();
		this.template = database.getView("XSLXMLCollection").getDocumentByKey("XSL: backup",true);
		
		this.backupType = profileDoc.getItemValueString("backupType");
		this.backupPath = profileDoc.getItemValueString("backupPath");
		if ((this.backupPath.indexOf(":\\") == -1) && (this.backupPath.indexOf("\\\\") == -1)) {
			this.backupPath = session.getEnvironmentString("Directory",true) + this.backupPath;
		}
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        this.backupVersion = dateFormat.format(new Date());
		this.backupName = "backup_"+this.backupVersion+".xml";
	}
	
	
	public String backup(Session session) throws Exception {
		System.out.println("Backup Data Entry to: "+this.backupPath);
		
		String returnValue = "";
		String tmpDir =  getTempDir();
				
		DxlExporter exporter = session.createDxlExporter();
		TransformerFactory tFactory = TransformerFactory.newInstance("org.apache.xalan.processor.TransformerFactoryImpl", null);
		
		Item rit = (RichTextItem)template.getFirstItem("Body");
		Transformer transformer = tFactory.newTransformer(new StreamSource(rit.getInputStream()));
	
		Database dataentryDatabase = session.getDatabase(null,profileDoc.getItemValueString("dataentryDatabase"));
	
		if (dataentryDatabase!=null) {
			DocumentCollection coll = dataentryDatabase.getView(template.getItemValueString("SearchViewName")).getAllDocumentsByKey("*",false);
			if (!template.getItemValueString("SearchQuery").equals("")) coll.FTSearch(template.getItemValueString("SearchQuery"));
			
		    FileOutputStream outFile = new FileOutputStream(tmpDir+this.backupName);
			Result result = new StreamResult(outFile);
			transformer.transform(new StreamSource(new StringReader(exporter.exportDxl(coll))), result);
			
			outFile.close();
			returnValue = zipBackup(tmpDir);
			returnValue = storeBackup(tmpDir);
			
			System.out.println("Finished backup Data Entry to: "+returnValue);
		
			coll.recycle();
			dataentryDatabase.recycle();
		}
		
		rit.recycle();
		return returnValue;
	}
	
	public String getSchedule() throws NotesException {
		return profileDoc.getItemValueString("backupSchedule");
	}
	
	public String getRemovePeriod() throws NotesException {
		return profileDoc.getItemValueString("backupRemove");
	}
	
	public String getBackupType() {
		return backupType;
	}
	
	public boolean remove(String filePath) {
	    System.out.println("Backup file removed: "+filePath);	
		File f = new File(filePath);
	    return  f.delete();
	}
	
	private String zipBackup(String filePath) throws IOException{
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(filePath+backupName+".zip")));
        out.setComment("DNV Navigator backup: "+backupVersion);
        
        // now adding files -- any number with putNextEntry() method
        BufferedReader in = new BufferedReader(new FileReader(filePath+backupName));
        out.putNextEntry(new ZipEntry(backupName));
        int c;
        while ((c = in.read()) != -1) out.write(c);
        in.close();
        out.close();

        File f = new File(filePath+backupName);
        f.delete();
        
        return filePath+backupName+".zip";
	}
	
	private String storeBackup(String filePath) throws Exception {
		String returnValue = "";
		File f = new File(filePath+backupName+".zip");
		if (backupType.equals("file")) {
			FileUtils.copyFile(f, new File(backupPath+backupName+".zip"));
			returnValue = backupPath+backupName+".zip";
		} else if (backupType.equals("ftp")) {
			
				} else if (backupType.equals("mail")) {
				
						}
		f.delete();
		return returnValue;
	}
	
	/*
	Return the full path to our temp directory as a String (with a
	file separator at the end)
	*/
	private String getTempDir () throws IOException{
		
		File foo = File.createTempFile("Foo", ".tmp");
		File tempDir = foo.getParentFile();
		foo.delete();

		// make sure we have a file separator character at the end of
		// the return String, for consistency
		String tempDirString = tempDir.getAbsolutePath();
		if (!tempDirString.endsWith(File.separator))
			tempDirString += File.separator;		

		return tempDirString;
	}
}
