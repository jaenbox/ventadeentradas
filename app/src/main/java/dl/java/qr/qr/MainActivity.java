package dl.java.qr.qr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.webkit.URLUtil;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.BufferedOutputStream;
import java.net.URLEncoder;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import android.app.AlertDialog;

public class MainActivity extends AppCompatActivity {

    private CameraSource cameraSource;
    private SurfaceView cameraView;
    private final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private String token = "";
    private String tokenanterior = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = (SurfaceView) findViewById(R.id.camera_view);
        initQR();
    }

    public void initQR() {

        // creo el detector qr
        BarcodeDetector barcodeDetector =
                new BarcodeDetector.Builder(this)
                        .setBarcodeFormats(Barcode.ALL_FORMATS)
                        .build();

        // creo la camara
        cameraSource = new CameraSource
                .Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1600, 1024)
                .setAutoFocusEnabled(true) //you should add this feature
                .build();

        // listener de ciclo de vida de la camara
        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

                // verifico si el usuario dio los permisos para la camara
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        // verificamos la version de ANdroid que sea al menos la M para mostrar
                        // el dialog de la solicitud de la camara
                        if (shouldShowRequestPermissionRationale(
                                Manifest.permission.CAMERA)) ;
                        requestPermissions(new String[]{Manifest.permission.CAMERA},
                                MY_PERMISSIONS_REQUEST_CAMERA);
                    }
                    return;
                } else {
                    try {
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException ie) {
                        Log.e("CAMERA SOURCE", ie.getMessage());
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        // preparo el detector de QR
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            private String readStream(InputStream is) throws IOException {
                StringBuilder sb = new StringBuilder();
                BufferedReader r = new BufferedReader(new InputStreamReader(is),1000);
                for (String line = r.readLine(); line != null; line =r.readLine()){
                    sb.append(line);
                }
                is.close();
                return sb.toString();
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                if (barcodes.size() > 0) {

                    // obtenemos el token
                    token = barcodes.valueAt(0).displayValue.toString();

                    // verificamos que el token anterior no se igual al actual
                    // esto es util para evitar multiples llamadas empleando el mismo token
                    if (!token.equals(tokenanterior)) {

                        // guardamos el ultimo token proceado
                        tokenanterior = token;
                        Log.i("token", token);
                        //if (URLUtil.isValidUrl(token)) {
                        if (URLUtil.isValidUrl(token)) {
                            URL url = null;
                            try {
                                url = new URL(token);
                                //url = new URL("https://www.growupservices.es/API_venta/?id=9-ZDQYI7CDVLXB");
                                Log.i("URL", String.valueOf(url));
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                                Log.i("Exception URL", "Estas jodido");
                            }
                            HttpURLConnection urlConnection = null;
                            try {
                                urlConnection = (HttpURLConnection) url.openConnection();
                                Log.i("URL", String.valueOf(urlConnection));
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.i("Exception URLConnection", "Estas jodido");
                            }
                            try {
                                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                                String respuesta = readStream(in);
                                Log.i("Respuesta", String.valueOf(respuesta));
                                // 1. Instantiate an AlertDialog.Builder with its constructor
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                                // 2. Chain together various setter methods to set the dialog characteristics
                                builder.setMessage(respuesta)
                                        .setTitle(respuesta);

                                // 3. Get the AlertDialog from create()
                                AlertDialog dialog = builder.create();

                                // Acciones a realizar con el flujo de datos
                                // Construir los datos a enviar
                                /*
                                String data = "body=" + URLEncoder.encode('Soluntec',"UTF-8");

                                urlConnection = (HttpURLConnection)url.openConnection();

                                // Activar método POST
                                urlConnection.setDoOutput(true);

                                // Tamaño previamente conocido
                                urlConnection.setFixedLengthStreamingMode(data.getBytes().length);

                                // Establecer application/x-www-form-urlencoded debido a la simplicidad de los datos
                                urlConnection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");

                                OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());

                                out.write(data.getBytes());
                                out.flush();
                                out.close();
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(token));
                                startActivity(browserIntent);
                                */
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.i("Exception Respuesta", "Estas jodido");
                            } finally {
                                urlConnection.disconnect();
                            }
                        }

                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    synchronized (this) {
                                        wait(5000);
                                        // limpiamos el token
                                        tokenanterior = "";
                                    }
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    Log.e("Error", "Waiting didnt work!!");
                                    e.printStackTrace();
                                }
                            }
                        }).start();

                    }
                }
            }
        });

    }

}