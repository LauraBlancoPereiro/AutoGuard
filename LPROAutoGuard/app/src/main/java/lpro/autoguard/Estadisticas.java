package lpro.autoguard;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class Estadisticas extends AppCompatActivity {

    private Context context;
    private LineChart chart;
    private View v;
    private FileJson fj;
    private ArrayList<Entry> low, high, medium;
    private int[] distraccionesAltasPorHora = new int[24];
    private int[] distraccionesMediasPorHora = new int[24];
    private int[] distraccionesLevesPorHora = new int[24];

    private int[] distraccionesAltasPorDia = new int[7];
    private int[] distraccionesMediasPorDia = new int[7];
    private int[] distraccionesLevesPorDia = new int[7];

    private boolean horaChart = true;

    public Estadisticas(Context c, View v, FileJson fj) {
        context = c;
        this.v = v;
        this.fj = fj;
        chart = (LineChart) v;

        XAxis x = chart.getXAxis();
        YAxis y = chart.getAxisLeft();
        YAxis y2 = chart.getAxisRight();
        y2.setEnabled(false);
        Legend legend = chart.getLegend();
        legend.setTextColor(context.getResources().getColor(R.color.blanco));
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setAxisLineWidth(2f);
        y.setAxisLineWidth(2f);
        x.setAxisLineColor(context.getResources().getColor(R.color.blanco));
        x.setTextColor(context.getResources().getColor(R.color.blanco));
        x.setGridColor(Color.argb(70, Color.red(context.getResources().getColor(R.color.gris_claro)),
                Color.green(context.getResources().getColor(R.color.gris_claro)),
                Color.blue(context.getResources().getColor(R.color.gris_claro))));

        y.setAxisLineColor(context.getResources().getColor(R.color.blanco));
        y.setTextColor(context.getResources().getColor(R.color.blanco));
        y.setGridColor(Color.argb(70, Color.red(context.getResources().getColor(R.color.gris_claro)),
                Color.green(context.getResources().getColor(R.color.gris_claro)),
                Color.blue(context.getResources().getColor(R.color.gris_claro))));

        chart.setBackgroundColor(context.getResources().getColor(R.color.gris_oscuro));

        chart.animateXY(2000, 2000);
        setDistraccionHoraChart();
    }

    public void changeChart(){
        if(horaChart){
            horaChart = false;
            setDistraccionDiaChart();

        }else{
            horaChart = true;
            setDistraccionHoraChart();

        }
    }
    public boolean getHoraChart(){
        return horaChart;
    }

    private void setDistraccionHoraChart(){

        parseJsonHora();
        startDataSets(chart);
        Description d = new Description();
        d.setText("Distracciones por hora");
        d.setTextColor(context.getResources().getColor(R.color.naranja));
        d.setTextSize(10f);
        chart.setDescription(d);
        chart.animateXY(2000, 2000);
        chart.invalidate(); // refresh

    }

    private void setDistraccionDiaChart(){

        parseJsonDia();
        startDataSets(chart);
        Description d = new Description();
        d.setText("Distracciones por dia");
        d.setTextColor(context.getResources().getColor(R.color.naranja));
        d.setTextSize(10f);
        chart.setDescription(d);
        chart.animateXY(1000, 1000);
        chart.invalidate(); // refresh

    }

    private void startDataSets(LineChart chart){

        LineDataSet dataSetA = new LineDataSet(high, "Distracciones Altas");
        dataSetA.setColor(context.getResources().getColor(R.color.naranja));
        dataSetA.setCircleColor(context.getResources().getColor(R.color.blanco));
        dataSetA.setCircleHoleColor(context.getResources().getColor(R.color.gris_claro));
        dataSetA.setValueTextColor(context.getResources().getColor(R.color.naranja));
        dataSetA.setValueTextSize(10f);
        dataSetA.setCircleRadius(4f);
        dataSetA.setLineWidth(2f);
        LineData lineDataA = new LineData(dataSetA);
        dataSetA.setDrawFilled(true);
        dataSetA.setFillFormatter(new IFillFormatter() {
            @Override
            public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
                return chart.getAxisLeft().getAxisMinimum();
            }
        });
        dataSetA.setFillDrawable(context.getResources().getDrawable(R.drawable.naranja_gradiente));

        LineDataSet dataSetM = new LineDataSet(medium, "Distracciones Medias");
        dataSetM.setColor(context.getResources().getColor(R.color.blanco));
        dataSetM.setCircleColor(context.getResources().getColor(R.color.gris_claro));
        dataSetM.setCircleHoleColor(context.getResources().getColor(R.color.naranja));
        dataSetM.setValueTextColor(context.getResources().getColor(R.color.blanco));
        dataSetM.setValueTextSize(10f);
        dataSetM.setCircleRadius(4f);
        dataSetM.setLineWidth(2f);
        LineData lineDataM = new LineData(dataSetM);
        dataSetM.setDrawFilled(true);
        dataSetM.setFillFormatter(new IFillFormatter() {
            @Override
            public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
                return chart.getAxisLeft().getAxisMinimum();
            }
        });
        dataSetM.setFillDrawable(context.getResources().getDrawable(R.drawable.blanco_gradiente));

        LineDataSet dataSetL = new LineDataSet(low, "Distracciones Leves");
        dataSetL.setColor(context.getResources().getColor(R.color.gris_claro));
        dataSetL.setCircleColor(context.getResources().getColor(R.color.naranja));
        dataSetL.setCircleHoleColor(context.getResources().getColor(R.color.blanco));
        dataSetL.setValueTextColor(context.getResources().getColor(R.color.gris_claro));
        dataSetL.setValueTextSize(10f);
        dataSetL.setCircleRadius(4f);
        dataSetL.setLineWidth(2f);
        LineData lineDataL = new LineData(dataSetL);
        dataSetL.setDrawFilled(true);
        dataSetL.setFillFormatter(new IFillFormatter() {
            @Override
            public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
                return chart.getAxisLeft().getAxisMinimum();
            }
        });
        dataSetL.setFillDrawable(context.getResources().getDrawable(R.drawable.gris_claro_gradiente));

        LineData data = new LineData(dataSetA, dataSetM, dataSetL);
        chart.setData(data);

    }

    private void parseJsonHora(){

        high = new ArrayList<>();
        low = new ArrayList<>();
        medium = new ArrayList<>();
        distraccionesAltasPorHora = new int[24];
        distraccionesMediasPorHora = new int[24];
        distraccionesLevesPorHora = new int[24];
        String hora = null;
        String gravedad = null;

        try {
            ArrayList<JSONObject> jsonArray = fj.readJsonFromFile();
            for (int i = 0; i < jsonArray.size() ; i++) {
                JSONObject jsonObject = jsonArray.get(i);
                gravedad = jsonObject.getString("Gravedad");
                hora = jsonObject.getString("Hora");

                switch(gravedad) {
                    case "low":
                        distraccionesLevesPorHora[Integer.parseInt(hora.split(":")[0])]++;
                        break;
                    case "medium":
                        distraccionesMediasPorHora[Integer.parseInt(hora.split(":")[0])]++;
                        break;
                    case "high":
                        distraccionesAltasPorHora[Integer.parseInt(hora.split(":")[0])]++;
                        break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for(int i = 0; i < distraccionesAltasPorHora.length; i++) {
            high.add(new Entry(i, distraccionesAltasPorHora[i]));
            medium.add(new Entry(i, distraccionesMediasPorHora[i]));
            low.add(new Entry(i, distraccionesLevesPorHora[i]));
        }
    }

    private void parseJsonDia(){

        high = new ArrayList<>();
        low = new ArrayList<>();
        medium = new ArrayList<>();
        distraccionesAltasPorDia = new int[7];
        distraccionesMediasPorDia = new int[7];
        distraccionesLevesPorDia = new int[7];
        String gravedad = null;
        String fecha = null;
        int diaSemana = 0; //Domingo, lunes, martes, miercoles, jueves, viernes, sabado
        Calendar cal = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        Date date = null;

        try {
            ArrayList<JSONObject> jsonArray = fj.readJsonFromFile();
            for (int i = 0; i < jsonArray.size() ; i++) {
                JSONObject jsonObject = jsonArray.get(i);
                gravedad = jsonObject.getString("Gravedad");
                fecha = jsonObject.getString("Fecha");
                date = df.parse(fecha);
                cal.setTime(date);
                diaSemana = cal.get(Calendar.DAY_OF_WEEK);

                switch(gravedad) {
                    case "low":
                        distraccionesLevesPorDia[diaSemana - 1]++;
                        break;
                    case "medium":
                        distraccionesMediasPorDia[diaSemana - 1]++;
                        break;
                    case "high":
                        distraccionesAltasPorDia[diaSemana - 1]++;
                        break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }catch(java.text.ParseException e){
            e.printStackTrace();
        }

        for(int i = 0; i < distraccionesAltasPorDia.length; i++) {
            high.add(new Entry(i, distraccionesAltasPorDia[i]));
            medium.add(new Entry(i, distraccionesMediasPorDia[i]));
            low.add(new Entry(i, distraccionesLevesPorDia[i]));
        }

    }

}
