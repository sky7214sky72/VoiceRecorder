package com.example.com.jsh.pratice9999;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.text.format.Time;

public class MainActivity extends Activity {

    int SAMPLE_RATE = 44100;

    private AudioRecord mRecorder;
    private File mRecording;
    private short[] mBuffer;
    private final String startRecordingLabel = "Start recording";
    private final String stopRecordingLabel = "Stop recording";
    private boolean mIsRecording = false;
    private ProgressBar mProgressBar;
    float iGain = 1.0f;
    int iChannel = 0;
    CheckBox channel;
    Button sr;
    SeekBar mSeekBar;
    TextView mVolume;
    private static final String TAG_TEXT = "text";
    List<Map<String, Object>> dialogItemList;
    String[] srate = {"16000","22050","44100","48000"};
    protected int bitsPerSamples = 16;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        channel = (CheckBox) findViewById(R.id.checkBox2);
        sr = (Button) findViewById(R.id.checkBox3);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mSeekBar = (SeekBar)findViewById(R.id.seekbar);
        mVolume = (TextView)findViewById(R.id.volume);
        initRecorder();
        TextView text2 = (TextView) findViewById(R.id.text2);
        TextView text3 = (TextView) findViewById(R.id.text3);

        text2.setText("MONO");
        text3.setText(SAMPLE_RATE+"");
        final Button button = (Button) findViewById(R.id.start);
        button.setText(startRecordingLabel);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                // TODO Auto-generated method stub
                mVolume.setText("Now Gain : " + progress + "dB");
                iGain = (float) progress;
            }
        });

        channel.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            TextView text2 = (TextView) findViewById(R.id.text2);
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if(channel.isChecked()){
                    iChannel = 1;
                    text2.setText("STEREO");
                    changeRecorder(SAMPLE_RATE,iChannel);
                }else{
                    iChannel = 0;
                    text2.setText("MONO");
                    changeRecorder(SAMPLE_RATE,iChannel);
                }
            }
        });
        sr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog();
            }
        });

        dialogItemList = new ArrayList<>();
        for(int i=0;i<srate.length;i++){
            Map<String,Object> itemMap = new HashMap<>();
            itemMap.put(TAG_TEXT,srate[i]);
            dialogItemList.add(itemMap);
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (!mIsRecording) {
                    button.setText(stopRecordingLabel);
                    mIsRecording = true;
                    mRecorder.startRecording();
                    mRecording = getFile("raw");
                    startBufferedWrite(mRecording);
                } else {
                    button.setText(startRecordingLabel);
                    mIsRecording = false;
                    mRecorder.stop();
                    File waveFile = getFile("wav");
                    try {
                        rawToWave(mRecording, waveFile);
                    } catch (IOException e) {
                        Toast.makeText(MainActivity.this, e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                    Toast.makeText(MainActivity.this,
                            "Recorded to " + waveFile.getName(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        mRecorder.release();
        super.onDestroy();
    }

    private void initRecorder() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mBuffer = new short[bufferSize];
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);
    }

    private void changeRecorder(int samplerate,int ichannel){
        if(ichannel == 1){
            int bufferSize = AudioRecord.getMinBufferSize(samplerate,
                    AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            mBuffer = new short[bufferSize];
            mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, samplerate,
                    AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);
        }else{
            int bufferSize = AudioRecord.getMinBufferSize(samplerate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            mBuffer = new short[bufferSize];
            mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, samplerate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);
        }
    }

    private void startBufferedWrite(final File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataOutputStream output = null;
                try {
                    output = new DataOutputStream(new BufferedOutputStream(
                            new FileOutputStream(file)));
                    while (mIsRecording) {
                        double sum = 0;

                        int readSize = mRecorder.read(mBuffer, 0,
                                mBuffer.length);

                        final int bytesPerSample = bitsPerSamples / 8;
                        final int emptySpace = 64 - bitsPerSamples;
                        int byteIndex = 0;
                        int byteIndex2 = 0;
                        int temp = 0;
                        int mLeftTemp = 0;
                        int mRightTemp = 0;
                        int a = 0;
                        int x = 0;

                        for (int frameIndex = 0; frameIndex < readSize; frameIndex++) {

                            for (int c = 0; c < 1; c++) {

                                if (iGain != 1.0f) {

                                    long accumulator = 0;
                                    for (int b = 0; b < bytesPerSample-1; b++) {
                                        accumulator += ((long) (mBuffer[byteIndex++] & 0xFF)) << (b * 8 + emptySpace);
                                    }
    
                                    double sample = ((double) accumulator / (double) Long.MAX_VALUE);
                                    sample *= iGain;
                                    int intValue = (int) ((double) sample * (double) Integer.MAX_VALUE);
                                    for (int i = 0; i < bytesPerSample-2; i++) {
                                        mBuffer[i + byteIndex2] = (byte) (intValue >>> ((i + 2) * 8) & 0xff);
                                    }
                                    byteIndex2 += bytesPerSample;

                                }
                            }// end for(channel)

                            mBuffer[frameIndex] *=iGain;
                            if (mBuffer[frameIndex] > 32765) {
                                mBuffer[frameIndex] = 32767;

                            } else if (mBuffer[frameIndex] < -32767) {
                                mBuffer[frameIndex] = -32767;
                            }
                            output.writeShort(mBuffer[frameIndex]);
                            sum += mBuffer[frameIndex] * mBuffer[frameIndex];

                        }

                        if (readSize > 0) {
                            final double amplitude = sum / readSize;
                            mProgressBar.setProgress((int) Math.sqrt(amplitude));
                        }
                    }
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                } finally {
                    mProgressBar.setProgress(0);
                    if (output != null) {
                        try {
                            output.flush();
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        } finally {
                            try {
                                output.close();
                            } catch (IOException e) {
                                Toast.makeText(MainActivity.this, e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        }).start();
    }

    private void rawToWave(final File rawFile, final File waveFile)
            throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {

            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            if(iChannel == 0){
                writeShort(output, (short) 1); // number of channels
            }else{
                writeShort(output, (short) 2); // number of channels
            }
            writeInt(output, SAMPLE_RATE); // sample rate
            writeInt(output, SAMPLE_RATE * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);

            for (short s : shorts) {

                // Apply Gain
                /*
                 * s *= iGain; if(s>32767) { s=32767; } else if(s<-32768) {
                 * s=-32768; }
                 */
                bytes.putShort(s);
            }
            output.write(bytes.array());
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private File getFile(final String suffix) {
        Time time = new Time();
        time.setToNow();
        return new File(Environment.getExternalStorageDirectory(),
                time.format("%Y%m%d%H%M%S") + "." + suffix);
    }


    private void showAlertDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.alert_dialog, null);
        builder.setView(view);

        final ListView listview = (ListView)view.findViewById(R.id.listview_alterdialog_list);
        final AlertDialog dialog = builder.create();

        SimpleAdapter simpleAdapter = new SimpleAdapter(MainActivity.this, dialogItemList,
                R.layout.alert_dialog_row,
                new String[]{TAG_TEXT},
                new int[]{R.id.alertDialogItemTextView});

        listview.setAdapter(simpleAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView text3 = (TextView) findViewById(R.id.text3);
                SAMPLE_RATE = Integer.parseInt(srate[position]);
                text3.setText(SAMPLE_RATE+"");
                dialog.dismiss();
            }
        });

        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }


    private void writeInt(final DataOutputStream output, final int value)
            throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value)
            throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value)
            throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }
}

