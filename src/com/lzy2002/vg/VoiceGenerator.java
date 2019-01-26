/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lzy2002.vg;

import com.iflytek.cloud.speech.SpeechConstant;
import com.iflytek.cloud.speech.SpeechError;
import com.iflytek.cloud.speech.SpeechSynthesizer;
import com.iflytek.cloud.speech.SpeechUtility;
import com.iflytek.cloud.speech.SynthesizeToUriListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author liziy
 */
public class VoiceGenerator {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        if (args.length > 0) {
            gen(args[0], "xiaoyan", "10", new File("./tmp/"));
        } else {
            write(new File("./pack.voice"), new File("./tmp/"));
        }
    }

    static {
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(new File("APP")));
            SpeechUtility.createUtility(SpeechConstant.APPID + "=" + properties.getProperty("xfyun_APPID"));
        } catch (IOException ex) {
            Logger.getLogger(VoiceGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static boolean running = false;

    public static synchronized void gen(String text, String VoiceName, String Speed, File tmp) {
        running = true;
        tmp.mkdirs();
        //1.创建SpeechSynthesizer对象
        SpeechSynthesizer mTts = SpeechSynthesizer.createSynthesizer();
        mTts.setParameter(SpeechConstant.VOICE_NAME, VoiceName);//设置发音人
        mTts.setParameter(SpeechConstant.SPEED, Speed);//设置语速，范围0~100
        mTts.setParameter(SpeechConstant.VOLUME, "50");//设置音量，范围0~100
        SynthesizeToUriListener synthesizeToUriListener = new SynthesizeToUriListener() {
            //progress为合成进度0~100 
            public void onBufferProgress(int progress) {
            }

            public void onSynthesizeCompleted(String uri, SpeechError error) {
                running = false;
                if (error != null) {
                    System.err.println(error.getErrorDescription(true));
                }
            }

            @Override
            public void onEvent(int i, int i1, int i2, int i3, Object o, Object o1) {
            }
        };
        mTts.synthesizeToUri(text, new File(tmp, text + ".pcm").getAbsolutePath(), synthesizeToUriListener);
    }

    public static void play(byte[] data) {
        try {
            int offset = 0;
            int bufferSize = Integer.valueOf(String.valueOf(data.length));

            float sampleRate = 16000;
            int sampleSizeInBits = 16;
            int channels = 1;
            boolean signed = true;
            boolean bigEndian = false;
            AudioFormat af = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
            SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, af, bufferSize);
            SourceDataLine sdl = (SourceDataLine) AudioSystem.getLine(info);
            sdl.open(af);
            sdl.start();
            while (offset < data.length) {
                offset += sdl.write(data, offset, bufferSize);
            }
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public static void write(File target, File tmp) {
        tmp.mkdirs();
        File[] fs = tmp.listFiles();
        int k = 0;
        HashMap<String, PCMFile> map = new HashMap<>();
        for (File f1 : fs) {
            PCMFile pcm = new PCMFile();
            int offset = 0;
            int bufferSize = Integer.valueOf(String.valueOf(f1.length()));
            pcm.Data = new byte[bufferSize];
            InputStream in;
            try {
                in = new FileInputStream(f1);
                in.read(pcm.Data);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(VoiceGenerator.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(VoiceGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
            pcm.FileName = f1.getName();
            map.put(f1.getName(), pcm);
        }

//        PCMFiles pcmfs = new PCMFiles();
//        pcmfs.files = pcms;
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(target));
            oos.writeObject(map);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(VoiceGenerator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(VoiceGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void read(File file, String id) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            HashMap<String, PCMFile> pcmfs = (HashMap<String, PCMFile>) ois.readObject();
            play(pcmfs.get(id + ".pcm").Data);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(VoiceGenerator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(VoiceGenerator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(VoiceGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void read(byte[] data, String id) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
            HashMap<String, PCMFile> pcmfs = (HashMap<String, PCMFile>) ois.readObject();
            play(pcmfs.get(id + ".pcm").Data);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(VoiceGenerator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(VoiceGenerator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(VoiceGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
