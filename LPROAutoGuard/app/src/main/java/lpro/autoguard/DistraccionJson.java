package lpro.autoguard;

import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

public class DistraccionJson {

    private String gravedad;
    private String fecha;
    private String hora;

    public DistraccionJson(String gravedad) {
        this.gravedad = gravedad;
        this.fecha = obtenerFechaActual();
        this.hora = obtenerHoraActual();
    }

    private String obtenerFechaActual() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        return String.format("%02d/%02d/%04d", day, month, year);
    }

    private String obtenerHoraActual() {
        Calendar calendar = Calendar.getInstance();
        int hora = calendar.get(Calendar.HOUR_OF_DAY);
        int minutos = calendar.get(Calendar.MINUTE);
        return String.format("%02d:%02d", hora, minutos);
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("Gravedad", gravedad);
            jsonObject.put("Fecha", fecha);
            jsonObject.put("Hora", hora);
        }catch(JSONException e){
            e.printStackTrace();
        }
        return jsonObject;
    }
}
