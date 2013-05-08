package com.android.pdroidf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;

public class captureJPEG extends Activity {
	private static final String TAG = "captureJPEG"; 
	final Time startTime=new Time();
	final Time endTime=new Time();
	String picPath;
	Button buttonClick;
	String pdfFile;
	SharedPreferences prefs;

	public void setPDFFile() {
		Log.d(TAG, "setPDFFile");
		// Get the xml/preferences.xml preferences
		String destFolder=prefs.getString("keyFolderName", Environment.getExternalStorageDirectory().getAbsolutePath());
		String destFile=prefs.getString("keyFileName", getString(R.string.defaultPDFFile));
		pdfFile=destFolder+ File.separator	+ destFile;
		Log.d(TAG,pdfFile);
	}

	public boolean changeSettings() {
		Log.d(TAG, "Change Settings");
		Intent i = new Intent(this, PreferActivity.class);
		startActivity(i);
		return true;
	}

	public void showAbout() {
		Log.d(TAG, "In showAbout()");
		showDialog(0);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case 0:
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.aboutdialog);
			dialog.setTitle(getString(R.string.AboutTitle));

			TextView text = (TextView) dialog.findViewById(R.id.aboutText);
			text.setText(getString(R.string.AboutText));
			ImageView image = (ImageView) dialog.findViewById(R.id.aboutLogo);
			image.setImageResource(R.drawable.pdroidf);
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.pdroidmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.settingsMenu:
			changeSettings();
			return true;
		case R.id.aboutMenu:
			showAbout();
			return true;
		case R.id.exitMenu:
			exitApplication();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Called when the exit menu option is selected.
	 **/
	private void exitApplication() {
		this.finish();
	}

	/**
	 * Called when the activity is first created.
	 * */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Get the xml/preferences.xml preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		Editor ed=prefs.edit();
		String destFolder=prefs.getString("keyFolderName", "");
		String destFile=prefs.getString("keyFileName", "");
		if (destFolder=="") 
		{
			ed.putString("keyFolderName",Environment.getExternalStorageDirectory().getAbsolutePath());
			ed.commit();
		}
		if (destFile=="") 
		{
			ed.putString("keyFileName",getString(R.string.defaultPDFFile));
			ed.commit();
		}
		Log.d("Prefs",destFolder+"+++"+destFile);

		// Click button
		buttonClick = (Button) findViewById(R.id.buttonClick);
		buttonClick.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startTime.setToNow();
				picPath=Environment.getExternalStorageDirectory()+File.separator+"DCIM"+File.separator+"Camera";
				Intent intent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
				startActivityForResult(intent, 0);
			}
		});
		Log.d(TAG, "onCreate-ed"); 
	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		Log.d(TAG, "requestCode " + requestCode);
		Log.d(TAG, "resultCode " + resultCode);
		if (requestCode == 0) {
			if (resultCode!=  0)
			{
				Log.d(TAG,"camera error");
				(Toast.makeText(getApplicationContext(),"There was an error reading from the camera. Please retry.", Toast.LENGTH_LONG)).show();
				return;
			}
			setPDFFile();
			endTime.setToNow();
			//If over write mode is false ask  user before overwriting 
			if (prefs.getBoolean("keyOverwrite",false )==false)
			{
				Log.d(TAG,"overwrite mode false");
				if (new File(pdfFile).exists())
				{
					Log.d(TAG,"file exists");
					//Warn user if file exists
					TextView newFileLabel=new TextView(this);
					EditText newFileName=new EditText(this);
					LinearLayout alertLayout=new LinearLayout(this);
					alertLayout.setOrientation(LinearLayout.HORIZONTAL);
					alertLayout.addView(newFileLabel);
					alertLayout.addView(newFileName);
					
					//fl.addView(myView, new LayoutParams(MATCH_PARENT, WRAP_CONTENT));
					AlertDialog.Builder builder = new AlertDialog.Builder(captureJPEG.this);
					builder.setMessage(pdfFile+" "+getString(R.string.OverwriteWarning))
					.setCancelable(false)
					.setView(alertLayout)
					.setPositiveButton(R.string.PostiveText, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							createFile();//continue with creating file
							return;
						}
					})
					.setNegativeButton(R.string.NegativeText, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Log.d(TAG,"user cancelled dialog");
							dialog.cancel();//Close dialog
							return;//return to main window
						}
					});
					AlertDialog alert = builder.create();
					alert.show();
				}				
			}
			else createFile();//continue with creating file
		}
	}	

	/* Create the pdfFile 
	 */
	private void createFile()
	{
		File dir = new File(picPath);
		String[] children = dir.list(new JpegFilter());
		List<String> matchedFiles = new ArrayList<String>();
		Log.d(TAG, "Dir " + dir.getAbsolutePath());
		for(int i=0;i<children.length;i++)
		{
			//Get file modify time
			File file = new File(picPath+File.separator+children[i]);
			// Get the last modification information.
			long lastModified = file.lastModified();
			if ( (lastModified >=startTime.toMillis(false)) && (lastModified <=endTime.toMillis(false)) )
			{
				Log.d(TAG,"Match ayidichu " + children[i]);
				matchedFiles.add(picPath+File.separator+children[i]);
			}
		}

		String successMessage = getString(R.string.SuccessMessage) +" "+ pdfFile; //$NON-NLS-1$
		String failureMessage = getString(R.string.FailureMessage) +" "+ pdfFile; //$NON-NLS-1$
		if (convertToPDF(matchedFiles,pdfFile)) {
			(Toast.makeText(getApplicationContext(),
					successMessage, Toast.LENGTH_SHORT)).show();
		} else {
			(Toast.makeText(getApplicationContext(),
					failureMessage, Toast.LENGTH_SHORT)).show();
		}			

	}
	/**
	 * Converts the image in "img" to PDF and stores it as "pdfFile"
	 * 
	 * @param img
	 *            - Image to convert
	 * @param pdfFile
	 *            - PDF file to be generated
	 * @return status of PDF conversion
	 */
	public boolean convertToPDF(List<String> files, String pdfFile) {
		try {
			String authorName = getString(R.string.AuthorName);
			String promoterText = getString(R.string.PromoterText);

			// Open a new file for writing
			File newFile = new File(pdfFile);
			FileOutputStream pdfStream = new FileOutputStream(newFile);
			// Get a PDF document instance and write to it
			Document doc = new Document();
			com.itextpdf.text.pdf.PdfWriter.getInstance(doc, pdfStream);
			Paragraph pr = new Paragraph(promoterText);
			Iterator<String> itr=files.iterator();
			while(itr.hasNext())
			{	
				Log.d(TAG,"Imagename " + itr.toString() );
				Image img=Image.getInstance(new URL("file://" + itr.next()));

				doc.setPageSize(new Rectangle(img.getRight(), img.getTop()));
				doc.setMargins(0, 0, 0, 0);
				doc.addAuthor(authorName);
				doc.open();
				doc.add(img);
				doc.newPage();
			}
			doc.add(pr);
			Log.d(TAG,"Closing " + doc.toString() );
			//TODO: Delete JPEG files
			//TODO: Merge with PdroidF code (preferences)
			//TODO: Share with Bluetooth, email, Facebook etc
			//TODO: Advertisement
			//TODO: Donations
			//TODO: Page width + height (optional)
			//TODO: Hard coding to be removed
			//TODO: Memory optimization
			//TODO: File search in list - optimize!
			//TODO: Show progress bar (page 1 of N)
			doc.close();
		} catch (Exception e) {
			(Toast.makeText(getApplicationContext(),
					getString(R.string.UnknownException) + e.getMessage(), Toast.LENGTH_LONG)).show(); //$NON-NLS-1$
			e.printStackTrace();
			return (false);
		}
		return (true);
	}
}
class JpegFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {
		return (name.endsWith(".jpg"));
	}
}