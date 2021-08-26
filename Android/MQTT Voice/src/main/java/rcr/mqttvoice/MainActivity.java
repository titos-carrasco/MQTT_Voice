package rcr.mqttvoice;

import java.util.ArrayList;
import java.util.UUID;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.speech.RecognizerIntent;
//import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends Activity implements View.OnClickListener {
    private final int REQUEST_CODE = 1963;
    private TextView txtSpeech;
    private EditText txtServer;
    private EditText txtTopic;
    private TextView txtStatus;
    private MqttAndroidClient mqttAndroidClient = null;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        //Log.d( "onCreate", "Begin" );

        txtSpeech = findViewById( R.id.txtSpeech );
        txtServer = findViewById( R.id.txtServer );
        txtTopic  = findViewById( R.id.txtTopic );
        txtStatus  = findViewById( R.id.txtStatus );

        txtStatus.setText( getString( R.string.Disconnected ) );

        findViewById( R.id.btnSpeak ).setOnClickListener( this );

        //Log.d( "onCreate", "End" );
    }

    @Override
    protected void onStart() {
        //Log.d( "onStart", "Begin" );
        super.onStart();
        //Log.d( "onStart", "End" );
    }

    @Override
    protected void onResume() {
        //Log.d( "onResume", "Begin" );
        super.onResume();

        SharedPreferences sharedPref = getPreferences( Context.MODE_PRIVATE );
        String savedServer = sharedPref.getString( getString( R.string.SavedServerKey ), getString( R.string.DefaultServer ) );
        String savedTopic = sharedPref.getString( getString( R.string.SavedTopicKey ), getString( R.string.DefaultTopic ) );
        txtServer.setText( savedServer );
        txtTopic.setText( savedTopic );

        //Log.d( "onResume", "End" );
    }

    @Override
    protected void onPause() {
        //Log.d( "onPause", "Begin" );
        super.onPause();

        SharedPreferences sharedPref = getPreferences( Context.MODE_PRIVATE );
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString( getString( R.string.SavedServerKey ), txtServer.getText().toString() );
        editor.putString( getString( R.string.SavedTopicKey ), txtTopic.getText().toString() );
        editor.apply();

        //Log.d( "onPause", "End" );
    }

    @Override
    protected void onStop() {
        //Log.d( "onStop", "Begin" );
        super.onStop();

        txtSpeech.setText( null );
        mqttDisconnect();

        //Log.d( "onStop", "End" );
    }

    @Override
    protected void onDestroy() {
        //Log.d( "onDestroy", "Begin" );
        super.onDestroy();
        //Log.d( "onDestroy", "End" );
    }

    @Override
    public void onClick( View v ) {
        //Log.d( "onClick", "Begin" );
        if( v.getId() == R.id.btnSpeak ) {
            //ActivityCompat.requestPermissions( MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1234 );
            doSpeechRecognition();
        }
        //Log.d( "onClick", "End" );
    }

    /*
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if( requestCode == 1234 && grantResults[0] == PackageManager.PERMISSION_GRANTED )
            doSpeechRecognition();
    }
    */

    private void doSpeechRecognition() {
        //Log.d( "doSpeechRecognition", "Begin" );

        txtSpeech.setText( null );
        mqttConnect();

        Intent intent = new Intent( RecognizerIntent.ACTION_RECOGNIZE_SPEECH );
        intent.putExtra( RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM );

        CheckBox offlineMode = findViewById( R.id.offlineMode);
        //if( offlineMode.isChecked())
        //    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        //        intent.putExtra( RecognizerIntent.EXTRA_PREFER_OFFLINE, true );
        //    }

        try {
            startActivityForResult( intent, REQUEST_CODE );
        } catch ( ActivityNotFoundException e ) {
            //Log.d( "onMicClicked", "ActivityNotFoundException" );
            Toast.makeText( getApplicationContext(), getString( R.string.NotRecognizer ), Toast.LENGTH_SHORT ).show();
        }

        //Log.d( "doSpeechRecognition", "End" );
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        //Log.d( "onActivityResult", "Begin" );

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK ) {
            ArrayList<String> result = data.getStringArrayListExtra( RecognizerIntent.EXTRA_RESULTS );
            txtSpeech.setText( result.get( 0 ) );
            mqttPublish( txtTopic.getText().toString(), result.get(0) );
        }
        else {
            //Log.d( "onActivityResult", "Not Published" );
            Toast.makeText( getApplicationContext(), getString( R.string.NotPublished ), Toast.LENGTH_SHORT ).show();
        }

        super.onActivityResult( requestCode, resultCode, data );

        //Log.d( "onActivityResult", "End" );
    }

    private void mqttConnect() {
        //Log.d( "mqttConnect", "Begin" );

        String serverUri = "tcp://" + txtServer.getText().toString();
        if( mqttAndroidClient != null ) {
            if( !mqttAndroidClient.isConnected() || !mqttAndroidClient.getServerURI().equals( serverUri ) ) {
                mqttDisconnect();
            }
        }

        if( mqttAndroidClient == null ) {
            txtStatus.setText( getString( R.string.Connecting ) );

            String clientId = "VAM-" + UUID.randomUUID();
            mqttAndroidClient = new MqttAndroidClient( getApplicationContext(), serverUri, clientId );
            mqttAndroidClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete( boolean b, String s ) {
                    txtStatus.setText( getString( R.string.Connected ) );
                    Toast.makeText( getApplicationContext(), getString( R.string.ConnectCompleted ), Toast.LENGTH_SHORT ).show();
                }

                @Override
                public void connectionLost( Throwable throwable ) {
                    txtStatus.setText( getString( R.string.Disconnected ) );
                    Toast.makeText( getApplicationContext(), getString( R.string.ConnectionLost ), Toast.LENGTH_SHORT ).show();
                }

                @Override
                public void messageArrived( String topic, MqttMessage mqttMessage ) {
                    Toast.makeText( getApplicationContext(), getString( R.string.MessageArrived ), Toast.LENGTH_SHORT ).show();
                }

                @Override
                public void deliveryComplete( IMqttDeliveryToken iMqttDeliveryToken ) {
                    Toast.makeText( getApplicationContext(), getString( R.string.DeliveryCompleted ), Toast.LENGTH_SHORT ).show();
                }
            });

            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setAutomaticReconnect( false ) ;
            mqttConnectOptions.setCleanSession( true );
            try {
                mqttAndroidClient.connect( mqttConnectOptions, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess( IMqttToken asyncActionToken ) {
                        txtStatus.setText( getString( R.string.Connected ) );
                        Toast.makeText( getApplicationContext(), getString( R.string.ConnectSuccess ), Toast.LENGTH_SHORT ).show();
                    }

                    @Override
                    public void onFailure( IMqttToken asyncActionToken, Throwable exception ) {
                        txtStatus.setText( getString( R.string.Disconnected ) );
                        Toast.makeText( getApplicationContext(), exception.getMessage(), Toast.LENGTH_SHORT ).show();
                    }
                });
            } catch ( MqttException e ) {
                txtStatus.setText( e.getMessage() );
                //Log.d("mqttConnect", e.getMessage() );
            }
        }

        //Log.d( "mqttConnect", "End" );
    }

    private void mqttDisconnect() {
        //Log.d("mqttDisconnect", "Begin" );

        if( mqttAndroidClient != null ) {
            mqttAndroidClient.close();
            mqttAndroidClient = null;
            txtStatus.setText( getString( R.string.Disconnected ) );
        }

        //Log.d("mqttDisconnect", "End" );
    }

    private void mqttPublish( String topic, String payload ) {
        //Log.d("mqttPublish", "Begin");

        if( mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            MqttMessage message = new MqttMessage();
            message.setQos(0);
            message.setPayload(payload.getBytes());
            try {
                mqttAndroidClient.publish( topic, message );
                //Log.d( "mqttPublish", topic );
                //Log.d( "mqttPublish", payload );
                //Log.d( "mqttPublish", getString( R.string.Published ) );
            } catch ( MqttException e ) {
                //Log.d("mqttPublish", e.getMessage() );
            }
        }

        //Log.d("mqttPublish", "End");
    }
}
