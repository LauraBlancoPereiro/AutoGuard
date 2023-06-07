package lpro.autoguard;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
// ContentResolver dependency
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.solutioncore.CameraInput;
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

public class MainActivity extends AppCompatActivity{
    private static final String TAG = "MainActivity";

    private FileJson fj;
    private DistraccionJson high, medium, low;

    private NotificationSound leve, medio, alto, distraction, yaw, Roll, pitch;
    private FaceMeshResultGlRenderer fmrgr = new FaceMeshResultGlRenderer();
    private FaceMesh facemesh;
    private Estadisticas estadisticas;
    // Run the pipeline and the model inference on GPU or CPU.
    private static final boolean RUN_ON_GPU = true;



    private final Runnable runnableD = new Runnable() {
        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException c) {
                    System.out.println("Error con el hilo");
                }
                comprobarDistracciones();
            }
        }
    };

    private final Runnable runnableP = new Runnable() {
        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException c) {
                    System.out.println("Error con el hilo");
                }
                displayParam();
            }
        }
    };
    private Thread hiloD;
    private Thread hiloP;

    private boolean bDistraccionMedia, bDistraccionAlta;

    private enum InputSource {
        UNKNOWN,
        CAMERA,
    }
    private InputSource inputSource = InputSource.UNKNOWN;
    // Live camera demo UI and camera components.
    private CameraInput cameraInput;

    private SolutionGlSurfaceView<FaceMeshResult> glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fj = new FileJson("datos.json", this);
        leve = new NotificationSound("LEVE", "INFRACCIÓN LEVE", this);
        medio = new NotificationSound("MEDIA", "INFRACCIÓN MEDIA", this);
        alto = new NotificationSound("ALTA", "INFRACCIÓN GRAVE", this);
        distraction = new NotificationSound("LEVE", "Fallo distracción", this);
        yaw = new NotificationSound("LEVE", "Fallo yaw", this);
        Roll = new NotificationSound("LEVE", "Fallo roll", this);
        pitch = new NotificationSound("LEVE", "Fallo pitch", this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (inputSource == InputSource.CAMERA) {
            // Restarts the camera and the opengl surface rendering.
            cameraInput = new CameraInput(this);
            cameraInput.setNewFrameListener(textureFrame -> facemesh.send(textureFrame));
            glSurfaceView.post(this::startCamera);
            glSurfaceView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (inputSource == InputSource.CAMERA) {
            glSurfaceView.setVisibility(View.GONE);
            cameraInput.close();
        }
    }

    public void monitoreo(View v){

        setContentView(R.layout.monitoreo);
        setupLiveDemoUiComponents();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    public void settings(View v){
        setContentView(R.layout.settings);
        printSettingsValues();
    }

    public void changeChart(View v){
        Button horas = findViewById(R.id.horas);

        if(!estadisticas.getHoraChart()){
            horas.setText("Ver por Dias");
        }else{
            horas.setText("Ver por Horas");

        }
        estadisticas.changeChart();

    }

    public void about(View v){

        setContentView(R.layout.about);
    }

    public void estadisticas(View v){

        setContentView(R.layout.estadisticas);
        estadisticas = new Estadisticas(this, findViewById(R.id.chart), fj);

    }

    public void atras(View v){
        if (inputSource == InputSource.CAMERA) {
            glSurfaceView.setVisibility(View.GONE);
            hiloD.interrupt();
            hiloP.interrupt();
            cameraInput.close();
        }
        setContentView(R.layout.activity_main);
    }

    public void salir(View v){

        finish();
    }

    private void ocultarTeclado(EditText et){
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
    }

    public void setMouthRatio(View v){
        EditText et = (EditText) findViewById(R.id.ratioBostezo);
        fmrgr.setMinYawn(Integer.parseInt(et.getText().toString()));
        printSettingsValues();
        ocultarTeclado(et);
    }

    public void setMinFramesYawn(View v){
        EditText et = (EditText) findViewById(R.id.framesBostezo);
        fmrgr.setMinFramesYawn(Integer.parseInt(et.getText().toString()));
        printSettingsValues();
        ocultarTeclado(et);
    }

    public void setMaxFramesEyesClosed(View v){
        EditText et = (EditText) findViewById(R.id.framesOjosCerrados);
        fmrgr.setMaxFramesEyesClosed(Integer.parseInt(et.getText().toString()));
        printSettingsValues();
        ocultarTeclado(et);
    }

    public void setMinEyesClosed(View v){
        EditText et = (EditText) findViewById(R.id.ratioOjoCerrado);
        fmrgr.setMinEyesClosed(Double.parseDouble(et.getText().toString()));
        printSettingsValues();
        ocultarTeclado(et);
    }

    public void setMaxBlinksPerMinute(View v){
        EditText et = (EditText) findViewById(R.id.pestaneosMinuto);
        fmrgr.setMaxBlinksPerMinute(Integer.parseInt(et.getText().toString()));
        printSettingsValues();
        ocultarTeclado(et);
    }

    public void setYawnsToDrowsy(View v){
        EditText et = (EditText) findViewById(R.id.yawnsToDrowsy);
        fmrgr.setYawnsToDrowsy(Integer.parseInt(et.getText().toString()));
        printSettingsValues();
        ocultarTeclado(et);
    }

    private void printSettingsValues(){
        TextView left = (TextView) findViewById(R.id.settingsValueLeft);
        TextView right = (TextView) findViewById(R.id.settingsValueRight);
        left.setText("Ratio Boca: " + fmrgr.getMinYawn() + "\n\n" +
                     "Frames Bostezo: " + fmrgr.getMinFramesYawn() + "\n\n" +
                     "Frames Ojos Cerrados: " + fmrgr.getMaxFramesEyesClosed() + "\n\n");

        right.setText("Ratio Ojo Cerrado: " + fmrgr.getMinEyesClosed() + "\n\n" +
                      "Pestañeos Minuto: " + fmrgr.getMaxBlinksPerMinute() + "\n\n" +
                      "Bostezo para Cansancio: " + fmrgr.getYawnsToDrowsy() + "\n\n");

    }

    public void stopNotificationSound(View v){

        medio.stopNotification();
        alto.stopNotification();
        fmrgr.resetValues();

        if(bDistraccionMedia) {
            bDistraccionMedia = false;
            medium = new DistraccionJson("medium");
            fj.writeJsonToFile(medium.toJSONObject());
        }
        if(bDistraccionAlta) {
            bDistraccionAlta = false;
            high = new DistraccionJson("high");
            fj.writeJsonToFile(high.toJSONObject());
        }

    }


    public void botonVolumen(View v){
        TextView avisoVolumen ;
        avisoVolumen = (TextView) findViewById(R.id.avisoVolumen) ;
        Button aceptarVolumen= findViewById(R.id.volumen);


        if (avisoVolumen.getVisibility() == v.VISIBLE) {
            avisoVolumen.setVisibility(v.GONE);
            aceptarVolumen.setVisibility(v.GONE);

        }

    }



    private void displayParam(){
        TextView parametros = (TextView) findViewById(R.id.parametros);
        if(parametros != null) {
            parametros.setText("Boca: " + fmrgr.getMouthRatio() + "\n" +
                    "Ojo izquierdo: " + fmrgr.getLeftEyeRatio() + "\n" +
                    "Ojo derecho: " + fmrgr.getRightEyeRatio());
        }

    }

    private void comprobarDistracciones(){

        if(fmrgr.getYawn()){
            fmrgr.setYawn();
            pushNotification(leve);
            low = new DistraccionJson("low");
            fj.writeJsonToFile(low.toJSONObject());
        }
        if(fmrgr.getDrowsy()){

            bDistraccionMedia = fmrgr.getDrowsy();
            fmrgr.setDrowsy();
            pushNotification(medio);

        }
        if(fmrgr.getSleeping()){
            bDistraccionAlta = fmrgr.getSleeping();
            fmrgr.setSleeping();
            pushNotification(alto);
        }
        if(fmrgr.getDistraction()){

            fmrgr.setDistraction();
            pushNotification(distraction);
        }
        if(fmrgr.getPitchDistraction()){

            fmrgr.setPitchDistraction();
            pushNotification(pitch);
        }
        if(fmrgr.getRollDistraction()){

            fmrgr.setRollDistraction();
            pushNotification(Roll);
        }
        if(fmrgr.getYawDistraction()){

            fmrgr.setYawDistraction();
            //pushNotification(yaw);
        }
    }

    /** Sets up the UI components for the live demo with camera input. */
    private void setupLiveDemoUiComponents() {
        Button startCameraButton = findViewById(R.id.button_start_camera);
        startCameraButton.setOnClickListener(
                v -> {
                    /*if (inputSource == InputSource.CAMERA) {
                        return;
                    }*/
                    stopCurrentPipeline();
                    setupStreamingModePipeline(InputSource.CAMERA);
                });
    }

    /** Sets up core workflow for streaming mode. */
    private void setupStreamingModePipeline(InputSource inputSource) {
        this.inputSource = inputSource;
        // Initializes a new MediaPipe Face Mesh solution instance in the streaming mode.
        facemesh =
                new FaceMesh(
                        this,
                        FaceMeshOptions.builder()
                                .setStaticImageMode(false)
                                .setRefineLandmarks(true)
                                .setRunOnGpu(RUN_ON_GPU)
                                .build());
        facemesh.setErrorListener((message, e) -> Log.e(TAG, "MediaPipe Face Mesh error:" + message));

        if (inputSource == InputSource.CAMERA) {
            cameraInput = new CameraInput(this);
            cameraInput.setNewFrameListener(textureFrame -> facemesh.send(textureFrame));
        }

        // Initializes a new Gl surface view with a user-defined FaceMeshResultGlRenderer.
        glSurfaceView =
                new SolutionGlSurfaceView<>(this, facemesh.getGlContext(), facemesh.getGlMajorVersion());
        //fmrgr = new FaceMeshResultGlRenderer();
        glSurfaceView.setSolutionResultRenderer(fmrgr);
        glSurfaceView.setRenderInputImage(true);
        facemesh.setResultListener(
                faceMeshResult -> {
                    logNoseLandmark(faceMeshResult, /*showPixelValues=*/ false);
                    glSurfaceView.setRenderData(faceMeshResult);
                    glSurfaceView.requestRender();
                });

        // The runnable to start camera after the gl surface view is attached.
        // For video input source, videoInput.start() will be called when the video uri is available.
        if (inputSource == InputSource.CAMERA) {
            glSurfaceView.post(this::startCamera);
        }

        // Updates the preview layout.
        FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
        frameLayout.removeAllViewsInLayout();
        frameLayout.addView(glSurfaceView);
        glSurfaceView.setVisibility(View.VISIBLE);
        frameLayout.requestLayout();
        hiloP = new Thread(runnableP);
        hiloD = new Thread(runnableD);
        hiloD.start();
        hiloP.start();
    }

    private void startCamera() {
        cameraInput.start(
                this,
                facemesh.getGlContext(),
                CameraInput.CameraFacing.FRONT,
                glSurfaceView.getWidth(),
                glSurfaceView.getHeight());
    }

    private void stopCurrentPipeline() {
        if (cameraInput != null) {
            cameraInput.setNewFrameListener(null);
            cameraInput.close();
        }
        if (glSurfaceView != null) {
            glSurfaceView.setVisibility(View.GONE);
        }
        if (facemesh != null) {
            facemesh.close();
        }
    }

    private void logNoseLandmark(FaceMeshResult result, boolean showPixelValues) {
        if (result == null || result.multiFaceLandmarks().isEmpty()) {
            return;
        }
        NormalizedLandmark noseLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(1);
        // For Bitmaps, show the pixel values. For texture inputs, show the normalized coordinates.
        if (showPixelValues) {
            int width = result.inputBitmap().getWidth();
            int height = result.inputBitmap().getHeight();
            Log.i(
                    TAG,
                    String.format(
                            "MediaPipe Face Mesh nose coordinates (pixel values): x=%f, y=%f",
                            noseLandmark.getX() * width, noseLandmark.getY() * height));
        } else {
            Log.i(
                    TAG,
                    String.format(
                            "MediaPipe Face Mesh nose normalized coordinates (value range: [0, 1]): x=%f, y=%f",
                            noseLandmark.getX(), noseLandmark.getY()));
        }
    }

    public void pushNotification(NotificationSound nS){


        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";

        nS.startNotification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @SuppressLint("WrongConstant") NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_MAX);
            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Intent stopped = new Intent(this,MainActivity.class);
        stopped.setAction("test");
        if (stopped.getAction().equals(stopped)) {
            nS.stopNotification();
        }

        PendingIntent actionPendingIntent = PendingIntent.getActivity(this,1,stopped,PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                //.addAction(R.drawable.boton_cochitosinfondo, "ACEPTAR", actionPendingIntent)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.image_notification)
                .setContentTitle("Distracción de gravedad: " + nS.getGravity())
                .setSound(nS.getUri())
                .setContentText(nS.getMessage())
                .setContentInfo("SMART");

        Notification notification=new Notification(R.drawable.logo_simple_color_peque5,"Infracción",100);
        notification.sound = nS.getUri();
        notification.defaults = Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE;
        notificationManager.notify(/*notification id*/1, notificationBuilder.build());
    }
}


