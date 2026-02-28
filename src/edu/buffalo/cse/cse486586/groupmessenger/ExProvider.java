package edu.buffalo.cse.cse486586.groupmessenger;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class ExProvider extends ContentProvider {
	public static final String AUTHORITY = "edu.buffalo.cse.cse486586.groupmessenger.provider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

	private static final UriMatcher match=new UriMatcher(UriMatcher.NO_MATCH);
	static
	{
		//match.addURI(AUTHORITY, "*", 1);
		match.addURI(AUTHORITY, null, 1);
		//match.addURI(AUTHORITY, "#", 1);
	}
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		String fileName=values.getAsString("key");
		String valueInFile=values.getAsString("value");
		Context c=getContext();
		int u=match.match(uri);
		switch(u)
		{
		case 1:
		try {
			//System.out.println("opening file");
			FileOutputStream fos=c.openFileOutput(fileName,Context.MODE_PRIVATE);
			
			fos.write(valueInFile.getBytes());
		
			fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		break;
		default:
			break;
		}
		//System.out.println("123");
		return uri;
	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		//System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		String [] sar=new String[2];
		MatrixCursor m=new MatrixCursor(new String[]{"key","value"});
		String fileName=selection;
		Context c=getContext();
		String op=null;
		int u=match.match(uri);
		switch(u)
		{
		case 1:
		try {
			FileInputStream fis=c.openFileInput(fileName);
			byte [] b=new byte[fis.available()];
			while((fis.read(b)) != -1)
			{
				
			}
			op=new String(b);
			fis.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		break;
		default:
			break;
		}
	//	System.out.println(selection+"---"+op);
		sar[0]=selection;
		sar[1]=op;
		m.addRow(sar);
		return m;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

}
