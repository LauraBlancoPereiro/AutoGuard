package lpro.autoguard;

import android.opengl.GLES20;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.common.collect.ImmutableSet;
import com.google.mediapipe.solutioncore.ResultGlRenderer;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshConnections;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Arrays;

/** A custom implementation of {@link ResultGlRenderer} to render {@link FaceMeshResult}. */
public class FaceMeshResultGlRenderer implements ResultGlRenderer<FaceMeshResult> {


    //Constantes Variables
    private static  int MIN_YAWN = 3;
    private static  int MIN_FRAMES_YAWN = 20;
    private static  int YAWNS_TO_DROWSY = 3;
    private static  int MAX_FRAMES_EYES_CLOSED = 30;
    private static  double MIN_EYES_CLOSED = 0.5;
    private static  final int FRAMES_IN_1_MINUTE = 900;
    private static  int MAX_BLINKS_PER_MINUTE = 10;
    private static final int MAX_FRAMES_DISTRACTION = 60;
    private static final int MAX_FRAMES_PITCH = 60;
    private static final int MAX_FRAMES_YAW = 45;
    private static final int MAX_FRAMES_ROLL = 60;

    //Distractions
    private boolean sleeping = false;
    private boolean drowsy = false;
    private boolean yawn = false;
    private boolean distraction = false;
    private boolean pitch_distraction = false;
    private boolean yaw_distraction = false;
    private boolean roll_distraction = false;
    //variables del frame anterior
    private float lastMouthRatio = 0;
    //variables de medición
    private int yawnFrames = 0;
    private int sleepingFrame = 0;
    private int counterYawn = 0;
    private int blinkFrame = 0;//contador para saber cuantos fps está con los ojos cerrados
    private int counterBlink = 0;
    private int blinksPerHalfMinute = 0;//30fps * 60s/min = 1800 fpm
    private int distractionFrame = 0;
    private int pitchFrame = 0;
    private int yawFrame = 0;
    private int rollFrame = 0;

    //Variables condición
    private boolean close = false;
    //Notificaciones

    //Colors are in RGB-alpha
    private static final float r = 246f/255f;
    private static final float g = 110f/255f;
    private static final float b = 13f/255f;
    private static final float a = 1f;
    private static final float[] TESSELATION_COLOR = new float[] {r, g, b, a};
    private static final int TESSELATION_THICKNESS = 3;
    private static final float[] RIGHT_EYE_COLOR = new float[] {r, g, b, a};
    private static final int RIGHT_EYE_THICKNESS = 3;
    private static final float[] RIGHT_EYEBROW_COLOR = new float[] {r, g, b, a};
    private static final int RIGHT_EYEBROW_THICKNESS = 3;
    private static final float[] LEFT_EYE_COLOR = new float[] {r, g, b, a};
    private static final int LEFT_EYE_THICKNESS = 3;
    private static final float[] LEFT_EYEBROW_COLOR = new float[] {r, g, b, a};
    private static final int LEFT_EYEBROW_THICKNESS = 3;
    private static final float[] FACE_OVAL_COLOR = new float[] {r, g, b, a};
    private static final int FACE_OVAL_THICKNESS = 3;
    private static final float[] LIPS_COLOR = new float[] {r, g, b, a};
    private static final int LIPS_THICKNESS = 3;
    private static final String VERTEX_SHADER =
            "uniform mat4 uProjectionMatrix;\n"
                    + "attribute vec4 vPosition;\n"
                    + "void main() {\n"
                    + "  gl_Position = uProjectionMatrix * vPosition;\n"
                    + "}";
    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n"
                    + "uniform vec4 uColor;\n"
                    + "void main() {\n"
                    + "  gl_FragColor = uColor;\n"
                    + "}";
    private int program;
    private int positionHandle;
    private int projectionMatrixHandle;
    private int colorHandle;

    //Points
    private final List<Integer> mouth_draw = Arrays.asList(14, 13);
    private final List<Integer> left_eye_draw = Arrays.asList(145, 159);
    private final List<Integer> right_eye_draw = Arrays.asList(374, 386);
    private final List<Integer> right_iris = Arrays.asList(474, 475, 476, 477);
    private final List<Integer> left_iris = Arrays.asList(469, 470, 471, 472);

    //AUX
    float mouth_ratio, left_eye_ratio, right_eye_ratio;
    double roll_center = 0;
    double yaw_center = 0;
    int roll_timer = 0;
    int yaw_timer = 0;
    private boolean first_time_roll = true;
    private boolean first_time_yaw = true;
    DecimalFormat df = new DecimalFormat("0.000");

    public String getMouthRatio(){
        return df.format(mouth_ratio);
    }

    public String getLeftEyeRatio(){
        return df.format(left_eye_ratio);
    }

    public String getRightEyeRatio(){
        return df.format(right_eye_ratio);
    }

    public boolean getSleeping(){
        return sleeping;
    }
    public boolean getDistraction() { return distraction;}
    public boolean getPitchDistraction() { return pitch_distraction;}
    public boolean getYawDistraction() { return yaw_distraction;}
    public boolean getRollDistraction() { return roll_distraction;}

    public boolean getDrowsy(){
        return drowsy;
    }

    public boolean getYawn(){
        return yawn;
    }

    public int getMinFramesYawn(){
        return MIN_FRAMES_YAWN;
    }

    public int getMaxFramesEyesClosed(){
        return MAX_FRAMES_EYES_CLOSED;
    }

    public double getMinEyesClosed(){
        return MIN_EYES_CLOSED;
    }

    public int getMaxBlinksPerMinute(){
        return MAX_BLINKS_PER_MINUTE;
    }

    public int getYawnsToDrowsy(){
        return YAWNS_TO_DROWSY;
    }

    public int getMinYawn(){
        return MIN_YAWN;
    }

    public void setMinYawn(int minYawn){
        MIN_YAWN = minYawn;
    }

    public void setMinFramesYawn(int minFramesYawn){
        MIN_FRAMES_YAWN = minFramesYawn;
    }

    public void setYawnsToDrowsy(int yawnsToDrowsy){
        YAWNS_TO_DROWSY = yawnsToDrowsy;
    }

    public void setMaxFramesEyesClosed(int maxFramesEyesClosed){
        MAX_FRAMES_EYES_CLOSED = maxFramesEyesClosed;
    }

    public void setMinEyesClosed(double minEyesClosed){
        MIN_EYES_CLOSED = minEyesClosed;
    }

    public void setMaxBlinksPerMinute(int maxBlinksPerMinute){
        MAX_BLINKS_PER_MINUTE = maxBlinksPerMinute;
    }

    public void setYawn(){
        yawn = false;
    }

    public void setDrowsy(){
        drowsy = false;
    }

    public void setSleeping(){
        sleeping = false;
    }

    public void setDistraction() { distraction = false;}
    public void setPitchDistraction() { pitch_distraction = false;}
    public void setRollDistraction() { roll_distraction = false;}
    public void setYawDistraction() { yaw_distraction = false;}

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);//OpenGL ES 2.0 (Open Graphics Library)
        GLES20.glCompileShader(shader);
        return shader;
    }

    @Override
    public void setupRendering() {
        program = GLES20.glCreateProgram();
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        projectionMatrixHandle = GLES20.glGetUniformLocation(program, "uProjectionMatrix");
        colorHandle = GLES20.glGetUniformLocation(program, "uColor");
    }

    @Override
    public void renderResult(FaceMeshResult result, float[] projectionMatrix) {
        //Ratios
        float ry1, ry2, mouth_up, mouth_down, left_eye_up, left_eye_down, right_eye_up, right_eye_down,
                reference, left_iris_x, left_iris_y, right_iris_x, right_iris_y, nose_tip_x, nose_tip_y,
                nose_tip_z, mid_eye_x, mid_eye_y, mid_eye_z, left_ear_x, left_ear_y, right_ear_x, right_ear_y;
        double right_ratio, left_ratio, up_ratio, bottom_ratio, pitch, roll, yaw;

        if (result == null) {
            return;
        }
        GLES20.glUseProgram(program);
        GLES20.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0);

        int numFaces = result.multiFaceLandmarks().size();

        if (numFaces > 0) {

            drawLandmarks(
                    result.multiFaceLandmarks().get(0).getLandmarkList(),
                    FaceMeshConnections.FACEMESH_TESSELATION,
                    TESSELATION_COLOR,
                    TESSELATION_THICKNESS);
            drawLandmarks(
                    result.multiFaceLandmarks().get(0).getLandmarkList(),
                    FaceMeshConnections.FACEMESH_RIGHT_EYE,
                    RIGHT_EYE_COLOR,
                    RIGHT_EYE_THICKNESS);
            drawLandmarks(
                    result.multiFaceLandmarks().get(0).getLandmarkList(),
                    FaceMeshConnections.FACEMESH_RIGHT_EYEBROW,
                    RIGHT_EYEBROW_COLOR,
                    RIGHT_EYEBROW_THICKNESS);
            drawLandmarks(
                    result.multiFaceLandmarks().get(0).getLandmarkList(),
                    FaceMeshConnections.FACEMESH_LEFT_EYE,
                    LEFT_EYE_COLOR,
                    LEFT_EYE_THICKNESS);
            drawLandmarks(
                    result.multiFaceLandmarks().get(0).getLandmarkList(),
                    FaceMeshConnections.FACEMESH_LEFT_EYEBROW,
                    LEFT_EYEBROW_COLOR,
                    LEFT_EYEBROW_THICKNESS);
            drawLandmarks(
                    result.multiFaceLandmarks().get(0).getLandmarkList(),
                    FaceMeshConnections.FACEMESH_FACE_OVAL,
                    FACE_OVAL_COLOR,
                    FACE_OVAL_THICKNESS);
            drawLandmarks(
                    result.multiFaceLandmarks().get(0).getLandmarkList(),
                    FaceMeshConnections.FACEMESH_LIPS,
                    LIPS_COLOR,
                    LIPS_THICKNESS);
            if (result.multiFaceLandmarks().get(0).getLandmarkCount()
                    == FaceMesh.FACEMESH_NUM_LANDMARKS_WITH_IRISES) {
                drawLandmarks(
                        result.multiFaceLandmarks().get(0).getLandmarkList(),
                        FaceMeshConnections.FACEMESH_RIGHT_IRIS,
                        RIGHT_EYE_COLOR,
                        RIGHT_EYE_THICKNESS);
                drawLandmarks(
                        result.multiFaceLandmarks().get(0).getLandmarkList(),
                        FaceMeshConnections.FACEMESH_LEFT_IRIS,
                        LEFT_EYE_COLOR,
                        LEFT_EYE_THICKNESS);
            }

            //Reference
            ry1 = result.multiFaceLandmarks().get(0).getLandmarkList().get(5).getY();
            ry2 = result.multiFaceLandmarks().get(0).getLandmarkList().get(4).getY();
            reference = ry2 - ry1;
            //Mouth
            mouth_down = result.multiFaceLandmarks().get(0).getLandmarkList().get(mouth_draw.get(0)).getY();
            mouth_up = result.multiFaceLandmarks().get(0).getLandmarkList().get(mouth_draw.get(1)).getY();
            mouth_ratio = (mouth_down - mouth_up) / reference;
            yawn(mouth_ratio);
            //Left eye
            left_eye_down = result.multiFaceLandmarks().get(0).getLandmarkList().get(left_eye_draw.get(0)).getY();
            left_eye_up = result.multiFaceLandmarks().get(0).getLandmarkList().get(left_eye_draw.get(1)).getY();
            left_eye_ratio = (left_eye_down - left_eye_up) / reference;
            //Right eye
            right_eye_down = result.multiFaceLandmarks().get(0).getLandmarkList().get(right_eye_draw.get(0)).getY();
            right_eye_up = result.multiFaceLandmarks().get(0).getLandmarkList().get(right_eye_draw.get(1)).getY();
            right_eye_ratio = (right_eye_down - right_eye_up) / reference;

            blinks(left_eye_ratio,right_eye_ratio);
            sleeping(left_eye_ratio, right_eye_ratio);

            //Left iris center
            left_iris_x = (result.multiFaceLandmarks().get(0).getLandmarkList().get(469).getX() + result.multiFaceLandmarks().get(0).getLandmarkList().get(470).getX() + result.multiFaceLandmarks().get(0).getLandmarkList().get(471).getX() + result.multiFaceLandmarks().get(0).getLandmarkList().get(472).getX()) / 4;
            left_iris_y = (result.multiFaceLandmarks().get(0).getLandmarkList().get(469).getY() + result.multiFaceLandmarks().get(0).getLandmarkList().get(470).getY() + result.multiFaceLandmarks().get(0).getLandmarkList().get(471).getY() + result.multiFaceLandmarks().get(0).getLandmarkList().get(472).getY()) / 4;
            //Right iris center
            right_iris_x = (result.multiFaceLandmarks().get(0).getLandmarkList().get(474).getX() + result.multiFaceLandmarks().get(0).getLandmarkList().get(475).getX() + result.multiFaceLandmarks().get(0).getLandmarkList().get(476).getX() + result.multiFaceLandmarks().get(0).getLandmarkList().get(476).getX()) / 4;
            right_iris_y = (result.multiFaceLandmarks().get(0).getLandmarkList().get(474).getY() + result.multiFaceLandmarks().get(0).getLandmarkList().get(475).getY() + result.multiFaceLandmarks().get(0).getLandmarkList().get(476).getY() + result.multiFaceLandmarks().get(0).getLandmarkList().get(476).getY()) / 4;

            //Euclidean distance of each eye and then, a mean (first left and then right)
            right_ratio = (Math.sqrt(Math.pow((result.multiFaceLandmarks().get(0).getLandmarkList().get(133).getX() - left_iris_x),2) + Math.pow((result.multiFaceLandmarks().get(0).getLandmarkList().get(133).getY() - left_iris_y),2)) /  reference
                    + Math.sqrt(Math.pow((result.multiFaceLandmarks().get(0).getLandmarkList().get(263).getX() - right_iris_x),2) + Math.pow((result.multiFaceLandmarks().get(0).getLandmarkList().get(263).getY() - right_iris_y),2)) /  reference) / 2;
            left_ratio = (Math.sqrt(Math.pow((result.multiFaceLandmarks().get(0).getLandmarkList().get(33).getX() - left_iris_x),2) + Math.pow((result.multiFaceLandmarks().get(0).getLandmarkList().get(33).getY() - left_iris_y),2)) /  reference
                    + Math.sqrt(Math.pow((result.multiFaceLandmarks().get(0).getLandmarkList().get(362).getX() - right_iris_x),2) + Math.pow((result.multiFaceLandmarks().get(0).getLandmarkList().get(362).getY() - right_iris_y),2)) /  reference) / 2;
            up_ratio = (Math.sqrt(Math.pow((result.multiFaceLandmarks().get(0).getLandmarkList().get(27).getX() - left_iris_x),2) + Math.pow((result.multiFaceLandmarks().get(0).getLandmarkList().get(27).getY() - left_iris_y),2)) /  reference
                    + Math.sqrt(Math.pow((result.multiFaceLandmarks().get(0).getLandmarkList().get(257).getX() - right_iris_x),2) + Math.pow((result.multiFaceLandmarks().get(0).getLandmarkList().get(257).getY() - right_iris_y),2)) /  reference) / 2;
            bottom_ratio = (Math.sqrt(Math.pow((result.multiFaceLandmarks().get(0).getLandmarkList().get(23).getX() - left_iris_x),2) + Math.pow((result.multiFaceLandmarks().get(0).getLandmarkList().get(23).getY() - left_iris_y),2)) /  reference
                    + Math.sqrt(Math.pow((result.multiFaceLandmarks().get(0).getLandmarkList().get(253).getX() - right_iris_x),2) + Math.pow((result.multiFaceLandmarks().get(0).getLandmarkList().get(253).getY() - right_iris_y),2)) /  reference) / 2;

            gazeTracking(right_ratio, left_ratio, up_ratio, bottom_ratio);

            //HeadPose
            //Nose tip
            nose_tip_x = result.multiFaceLandmarks().get(0).getLandmarkList().get(1).getX();
            nose_tip_y = result.multiFaceLandmarks().get(0).getLandmarkList().get(1).getY();
            nose_tip_z = result.multiFaceLandmarks().get(0).getLandmarkList().get(1).getZ();
            //Midpoint between eyes
            mid_eye_x = (left_iris_x + right_iris_x)/2;
            mid_eye_y = (left_iris_y + right_iris_y)/2;
            mid_eye_z = ((result.multiFaceLandmarks().get(0).getLandmarkList().get(right_iris.get(0)).getZ() + result.multiFaceLandmarks().get(0).getLandmarkList().get(right_iris.get(1)).getZ() + result.multiFaceLandmarks().get(0).getLandmarkList().get(right_iris.get(2)).getZ() + result.multiFaceLandmarks().get(0).getLandmarkList().get(right_iris.get(3)).getZ()) / 4 + (result.multiFaceLandmarks().get(0).getLandmarkList().get(left_iris.get(0)).getZ() + result.multiFaceLandmarks().get(0).getLandmarkList().get(left_iris.get(1)).getZ() + result.multiFaceLandmarks().get(0).getLandmarkList().get(left_iris.get(2)).getZ() + result.multiFaceLandmarks().get(0).getLandmarkList().get(left_iris.get(3)).getZ()) / 4) / 2;
            //Left ear
            left_ear_x = result.multiFaceLandmarks().get(0).getLandmarkList().get(263).getX();
            left_ear_y = result.multiFaceLandmarks().get(0).getLandmarkList().get(263).getY();
            //Right ear
            right_ear_x = result.multiFaceLandmarks().get(0).getLandmarkList().get(33).getX();
            right_ear_y = result.multiFaceLandmarks().get(0).getLandmarkList().get(33).getY();

            //Measures
            pitch = Math.atan2((mid_eye_y - nose_tip_y) / reference, (mid_eye_z - nose_tip_z) / reference) * 180 / Math.PI;
            roll = Math.atan2((mid_eye_x - nose_tip_x) / reference, (mid_eye_z - nose_tip_z) / reference) * 180 / Math.PI;
            yaw = Math.atan2((left_ear_y - right_ear_y) / reference, (left_ear_x - right_ear_x) / reference) * 180 / Math.PI;

            headpose_pitch(pitch);
            headpose_yaw(yaw);
            headpose_roll(roll);
        }
    }

    public void resetValues(){
        sleeping = false;
        drowsy = false;
        yawn = false;
        lastMouthRatio = 0;
        yawnFrames = 0;
        counterYawn = 0;
        blinkFrame = 0;
        counterBlink = 0;
        blinksPerHalfMinute = 0;
    }

    private void blinks(float left_eye_ratio,float right_eye_ratio){
        blinkFrame++;

        if(left_eye_ratio <= MIN_EYES_CLOSED ){

            close = true;
        }
        if(close & left_eye_ratio > MIN_EYES_CLOSED ){

            counterBlink++;
            close = false;
        }
        if(blinkFrame == FRAMES_IN_1_MINUTE){

            blinksPerHalfMinute = counterBlink;
        }
        if(blinksPerHalfMinute >= MAX_BLINKS_PER_MINUTE){

            drowsy = true;
            blinkFrame = 0;
            blinksPerHalfMinute = 0;
            counterBlink = 0;
        }
    }

    private void yawn(float mouthRatio){

        if(mouthRatio >= MIN_YAWN){//cuando es mayor o igual al minimo empieza a contar frames

            yawnFrames++;
        }

        if(mouthRatio < MIN_YAWN){

            if(lastMouthRatio >= MIN_YAWN && yawnFrames >= MIN_FRAMES_YAWN){

                counterYawn++;
                yawn = true;

                if(counterYawn >= YAWNS_TO_DROWSY){

                    drowsy = true;
                    yawn = false;
                }
            }
            yawnFrames = 0;
        }
        lastMouthRatio = mouthRatio;
    }

    private void sleeping(float leftEyeRatio, float rightEyeRatio){

        if(leftEyeRatio <= MIN_EYES_CLOSED && rightEyeRatio <= MIN_EYES_CLOSED){

            sleepingFrame++;

            if(sleepingFrame >= MAX_FRAMES_EYES_CLOSED){

                sleeping = true;
            }
        }else{

            sleepingFrame = 0;
            sleeping = false;
        }

    }

    private void gazeTracking(double right_ratio, double left_ratio, double up_ratio, double bottom_ratio){
        double center_right = 2.25;
        double center_left = 1.60;
        double center_up = 1.6;
        double center_bottom = 1.30;

        if (right_ratio > center_right + 0.25){
            distractionFrame++;
            if (distractionFrame >= MAX_FRAMES_DISTRACTION) {
                distraction = true;
                System.out.println("Soy yo");
            }
        }
        else if(left_ratio > center_left + 0.65){
            distractionFrame++;
            if (distractionFrame >= MAX_FRAMES_DISTRACTION) {
                distraction = true;
                System.out.println("Soy yo");

            }
        }/*
        else if(up_ratio > center_up + 0.5){
            distractionFrame++;
            if (distractionFrame >= MAX_FRAMES_DISTRACTION) {
                distraction = true;
            }
        }
        else if(bottom_ratio > center_bottom + 0.15){
            distractionFrame++;
            if (distractionFrame >= MAX_FRAMES_DISTRACTION) {
                distraction = true;
            }
        }*/
        else {
            distractionFrame = 0;
            distraction = false;
        }
    }

    private void headpose_pitch(double pitch) {
        double pitch_center = -35;
        if (pitch > pitch_center + 10 || pitch < pitch_center - 10) {
            pitchFrame++;
            if (pitchFrame >= MAX_FRAMES_PITCH) {
                pitch_distraction = true;
            }
        } else {
            pitchFrame = 0;
            pitch_distraction = false;
        }
    }

    private void headpose_yaw(double yaw) {
        if (first_time_yaw) {
            if (yaw > 6) {
                yaw_center = 8;
                first_time_yaw = false;
            }
            else if (yaw < 4) {
                yaw_center = 2;
                first_time_yaw = false;
            }
            else {
                return;
            }
        }

        //Thus we can reset the central value in case the phone moves during the use of the APP
        yaw_timer += 1;
        if (yaw_timer == 100) {
            yaw_timer = 0;
            first_time_yaw = true;
        }

        if (yaw > yaw_center + 2.5 || yaw < yaw_center - 2.5) {
            yawFrame++;
            if (yawFrame >= MAX_FRAMES_YAW) {
                yaw_distraction = true;
            }
        } else {
            yawFrame = 0;
            yaw_distraction = false;
        }
    }

    private void headpose_roll(double roll) {
        if (first_time_roll) {
            if (roll >= -13 && roll <= 15) {
                roll_center = -3;
                first_time_roll = false;
            }
            else if (roll > 23) {
                roll_center = 30;
                first_time_roll = false;
            }
            else if (roll < -17) {
                roll_center = -33;
                first_time_roll = false;
            }
            else {
                return;
            }
        }
        //Thus we can reset the central value in case the phone moves during the use of the APP
        roll_timer += 1;
        if (roll_timer == 100) {
            roll_timer = 0;
            first_time_roll = true;
        }

        if (roll > roll_center + 15 || roll < roll_center - 15) {
            rollFrame++;
            if (rollFrame >= MAX_FRAMES_ROLL) {
                roll_distraction = true;
            }
        } else {
            rollFrame = 0;
            roll_distraction = false;
        }
    }

    /**
     * Deletes the shader program.
     *
     * <p>This is only necessary if one wants to release the program while keeping the context around.
     */
    public void release() {
        GLES20.glDeleteProgram(program);
    }

    private void drawLandmarks(
            List<NormalizedLandmark> faceLandmarkList,
            ImmutableSet<FaceMeshConnections.Connection> connections,
            float[] colorArray,
            int thickness) {

        GLES20.glUniform4fv(colorHandle, 1, colorArray, 0);
        GLES20.glLineWidth(thickness);

        for (FaceMeshConnections.Connection c : connections) {

            NormalizedLandmark start = faceLandmarkList.get(c.start());
            NormalizedLandmark end = faceLandmarkList.get(c.end());
            float[] vertex = {start.getX(), start.getY(), end.getX(), end.getY()};
            FloatBuffer vertexBuffer =
                    ByteBuffer.allocateDirect(vertex.length * 4)
                            .order(ByteOrder.nativeOrder())
                            .asFloatBuffer()
                            .put(vertex);
            vertexBuffer.position(0);
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);

        }
    }
}
