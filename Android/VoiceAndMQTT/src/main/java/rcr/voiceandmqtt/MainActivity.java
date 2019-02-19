package rcr.voiceandmqtt;

import java.util.ArrayList;
import java.util.UUID;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {
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

        Log.d( "onCreate", "Begin" );

        txtSpeech = findViewById( R.id.txtSpeech );
        txtServer = findViewById( R.id.txtServer );
        txtTopic  = findViewById( R.id.txtTopic );
        txtStatus  = findViewById( R.id.txtStatus );

        txtStatus.setText( getString( R.string.Disconnected ) );

        Log.d( "onCreate", "End" );
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d( "onStart", "Begin" );
        Log.d( "onStart", "End" );
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d( "onResume", "Begin" );

        SharedPreferences sharedPref = getPreferences( Context.MODE_PRIVATE );
        String savedServer = sharedPref.getString( getString( R.string.SavedServerKey ), getString( R.string.DefaultServer ) );
        String savedTopic = sharedPref.getString( getString( R.string.SavedTopicKey ), getString( R.string.DefaultTopic ) );
        txtServer.setText( savedServer );
        txtTopic.setText( savedTopic );

        Log.d( "onResume", "End" );
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d( "onPause", "Begin" );

        SharedPreferences sharedPref = getPreferences( Context.MODE_PRIVATE );
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString( getString( R.string.SavedServerKey ), txtServer.getText().toString() );
        editor.putString( getString( R.string.SavedTopicKey ), txtTopic.getText().toString() );
        editor.apply();

        Log.d( "onPause", "End" );
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d( "onStop", "Begin" );

        txtSpeech.setText( null );
        mqttDisconnect();

        Log.d( "onStop", "End" );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d( "onDestroy", "Begin" );
        Log.d( "onDestroy", "End" );
    }

    public void onMicClicked( View v ) {
        Log.d( "onMicClicked", "Begin" );

        txtSpeech.setText( null );
        mqttConnect();

        CheckBox offlineMode = findViewById( R.id.offlineMode);

        Intent intent = new Intent( RecognizerIntent.ACTION_RECOGNIZE_SPEECH );
        intent.putExtra( RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM );
        if( offlineMode.isChecked())
            intent.putExtra( RecognizerIntent.EXTRA_PREFER_OFFLINE, true );

        try {
            startActivityForResult( intent, REQUEST_CODE );
        } catch ( ActivityNotFoundException e ) {
            Log.d( "onMicClicked", "ActivityNotFoundException" );
            Toast.makeText( getApplicationContext(), getString( R.string.NotRecognizer ), Toast.LENGTH_SHORT ).show();
        }

        Log.d( "onMicClicked", "End" );
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult( requestCode, resultCode, data );

        Log.d( "onActivityResult", "Begin" );

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK ) {
            ArrayList<String> result = data.getStringArrayListExtra( RecognizerIntent.EXTRA_RESULTS );
            txtSpeech.setText( result.get( 0 ) );
            mqttPublish( txtTopic.getText().toString(), result.get(0) );
        }
        else
            Log.d( "onActivityResult", "Not Published" );

        Log.d( "onActivityResult", "End" );
    }

    private void mqttConnect() {
        Log.d( "mqttConnect", "Begin" );

        String serverUri = "tcp://" + txtServer.getText().toString();
        if( mqttAndroidClient != null ) {
            if( !mqttAndroidClient.isConnected() || !mqttAndroidClient.getServerURI().equals( serverUri ) ) {
                mqttDisconnect();
            }
        }

        if( mqttAndroidClient == null ) {
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
                    txtStatus.setText( getString( R.string.ConnectionLost ) );
                    Toast.makeText( getApplicationContext(), getString( R.string.ConnectionLost ), Toast.LENGTH_SHORT ).show();
                }

                @Override
                public void messageArrived( String topic, MqttMessage mqttMessage ) throws Exception {
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
                txtStatus.setText( getString( R.string.Connecting ) );
                mqttAndroidClient.connect( mqttConnectOptions, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess( IMqttToken asyncActionToken ) {
                        txtStatus.setText( getString( R.string.Connected ) );
                        Toast.makeText( getApplicationContext(), getString( R.string.ConnectSuccess ), Toast.LENGTH_SHORT ).show();
                    }

                    @Override
                    public void onFailure( IMqttToken asyncActionToken, Throwable exception ) {
                        txtStatus.setText( getString( R.string.ConnectFailure ) );
                        Toast.makeText( getApplicationContext(), getString( R.string.ConnectFailure ), Toast.LENGTH_SHORT ).show();
                    }
                });
            } catch ( MqttException e ) {
                txtStatus.setText( getString( R.string.Disconnected ) );
                Log.d("mqttConnect", "MqttException");
            }
        }

        Log.d( "mqttConnect", "End" );
    }

    private void mqttDisconnect() {
        Log.d("mqttDisconnect", "Begin" );

        if( mqttAndroidClient != null ) {
            mqttAndroidClient.close();
            mqttAndroidClient = null;
            txtStatus.setText( getString( R.string.Disconnected ) );
        }

        Log.d("mqttDisconnect", "End" );
    }

    private void mqttPublish( String topic, String payload ) {
        Log.d("mqttPublish", "Begin");

        try {
            MqttMessage message = new MqttMessage();
            message.setQos(0);
            message.setPayload( payload.getBytes() );
            mqttAndroidClient.publish( topic, message );
            Log.d( "mqttPublish", "Published" );
        } catch (MqttException e) {
            Log.d("mqttPublish", "MqttException");
        }

        Log.d("mqttPublish", "End");
    }

}

