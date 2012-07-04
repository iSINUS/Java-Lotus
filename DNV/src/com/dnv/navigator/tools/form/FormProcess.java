package com.dnv.navigator.tools.form;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.Item;
import lotus.domino.RichTextDoclink;
import lotus.domino.RichTextItem;
import lotus.domino.RichTextNavigator;
import lotus.domino.View;

public class FormProcess {
	private Database Database;
	private Document Form;
	private Document Template;

	public FormProcess( Database Database ) throws Exception {
		this.Database = Database;
	}

	public void run( DocumentCollection collection ) throws Exception {
		if( collection.getCount() == 0 )
			return;
		for( this.Form = collection.getFirstDocument(); Form != null; this.Form = collection.getNextDocument() ) {
			getTemplate();
			updateForm();
			updateTemplate();
		}
	}

	private void getTemplate() throws Exception {
		Document template = searchTemplate();
		if( template == null )
			template = createTemplate();
		this.Template = template;
	}

	private Document searchTemplate() throws Exception {
		View view = Database.getView( "XSLXMLCollection" );
		Document template = view.getDocumentByKey( getAttachmentType() + ": " + Form.getItemValueString( "TemplateCategory" ) + " \\ " + Form.getItemValueString( "TemplateName" ), true );
		return template;
	}

	private String getAttachmentType() throws Exception {
		return ( String ) Database.getParent().evaluate( "@Replace( @LowerCase( @RightBack( @Subset( @AttachmentNames; 1 ); \".\" ) ); \"pdf\" : \"doc\" : \"docx\" : \"dot\" : \"xsl\" : \"xls\" : \"xlsx\"; \"PDF\" : \"DOC\" : \"DOC\" : \"DOC\" : \"DOC\" : \"XLS\" : \"XLS\" )", Form ).get( 0 );
	}
	
	private Document createTemplate() throws Exception {
		Document template = Database.createDocument();
		template.replaceItemValue( "Form", "XSL" );
		template.replaceItemValue( "Type", getAttachmentType() );
		template.replaceItemValue( "SearchIn", "View" );
		template.computeWithForm( true, true );
		return template;
	}

	private void updateForm() throws Exception {
		boolean isUpdate = false;
		isUpdate |= updateField( Form.getFirstItem( "TemplateXML" ), getAttachmentType() + ": " + Form.getItemValueString( "TemplateCategory" ) + " \\ " + Form.getItemValueString( "TemplateName" ) );
		isUpdate |= updateField( Form.getFirstItem( "TemplateLink" ), Template );
		if( isUpdate )
			Form.save();
	}

	private void updateTemplate() throws Exception {
		boolean isUpdate = false;
		isUpdate |= updateField( Template.getFirstItem( "Name" ), Form.getItemValueString( "TemplateCategory" ) + " \\ " + Form.getItemValueString( "TemplateName" ) );
		isUpdate |= updateField( Template.getFirstItem( "FormLink" ), Form );
		if( isUpdate )
			Template.save();
	}

	private boolean updateField( Item item, Object value ) throws Exception {
		if( item.containsValue( value ) )
			return false;
		item.getParent().replaceItemValue( item.getName(), value );
		return true;
	}

	private boolean updateField( Item item, Document link ) throws Exception, Exception {
		if( isContainDocLink( item, link.getUniversalID() ) )
			return false;
		replaceDocLink( item, link );
		return true;
	}

	private boolean isContainDocLink( Item field, String unid ) throws Exception {
		if( field.isSummary() )
			return false;
		RichTextNavigator navigator = ( ( RichTextItem ) field ).createNavigator();
		if( navigator.findFirstElement( RichTextItem.RTELEM_TYPE_DOCLINK ) ) {
			do {
				RichTextDoclink link = ( RichTextDoclink ) navigator.getElement();
				if( unid.compareTo( link.getDocUnID() ) == 0 )
					return true;
			} while( navigator.findNextElement() );
		}
		return false;
	}

	private void replaceDocLink( Item field, Document link ) throws Exception {
		Document doc = field.getParent();
		String name = field.getName();
		if( doc.hasItem( name ) )
			doc.removeItem( name );
		RichTextItem item = doc.createRichTextItem( name );
		item.appendDocLink( link );
	}
}