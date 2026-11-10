package com.ancylostoma.tutor;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Aviso diario: a la hora elegida muestra qué actividades están vencidas, cuáles
 * vencen hoy y cuáles tocan según el plan. Funciona con la app cerrada y sin
 * internet: la web le deja la lista guardada cada vez que cambian los datos.
 */
public class Recordatorio extends BroadcastReceiver {

  private static final String PREFS = "recordatorio";
  private static final String CANAL = "entregas";
  private static final int ID_AVISO = 1;

  @Override
  public void onReceive(Context context, Intent intent) {
    // Al encender el teléfono solo hay que volver a programar la alarma.
    if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
      try {
        mostrar(context);
      } catch (Exception ignorada) {
        // Un aviso fallido no debe impedir programar el de mañana.
      }
    }
    programar(context);
  }

  /** La web llama aquí cada vez que guarda: {activo, hora, minuto, items:[{t,a,f,p}]}. */
  static void guardar(Context context, String json) {
    prefs(context).edit().putString("datos", json).apply();
    programar(context);
  }

  private static SharedPreferences prefs(Context context) {
    return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }

  private static JSONObject leer(Context context) {
    try {
      return new JSONObject(prefs(context).getString("datos", "{}"));
    } catch (Exception e) {
      return new JSONObject();
    }
  }

  private static String hoy() {
    return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
  }

  static void programar(Context context) {
    AlarmManager alarmas = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (alarmas == null) return;
    PendingIntent pendiente = PendingIntent.getBroadcast(context, 0,
        new Intent(context, Recordatorio.class),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    JSONObject datos = leer(context);
    if (!datos.optBoolean("activo", false)) {
      alarmas.cancel(pendiente);
      return;
    }

    Calendar cuando = Calendar.getInstance();
    cuando.set(Calendar.HOUR_OF_DAY, datos.optInt("hora", 8));
    cuando.set(Calendar.MINUTE, datos.optInt("minuto", 0));
    cuando.set(Calendar.SECOND, 0);
    cuando.set(Calendar.MILLISECOND, 0);
    if (!cuando.after(Calendar.getInstance())) {
      // La hora de hoy ya pasó: si hoy aún no se avisó (alarma retrasada por el
      // ahorro de batería o aviso recién activado), se avisa en un minuto.
      if (hoy().equals(prefs(context).getString("ultimo", ""))) {
        cuando.add(Calendar.DAY_OF_YEAR, 1);
      } else {
        cuando = Calendar.getInstance();
        cuando.add(Calendar.MINUTE, 1);
      }
    }
    // Alarma inexacta: no necesita el permiso especial de alarmas exactas.
    alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando.getTimeInMillis(), pendiente);
  }

  private static void mostrar(Context context) throws Exception {
    String hoy = hoy();
    prefs(context).edit().putString("ultimo", hoy).apply();

    JSONArray items = leer(context).optJSONArray("items");
    if (items == null) return;

    int vencidas = 0, vencenHoy = 0, tocan = 0;
    List<String> lineas = new ArrayList<>();
    for (int i = 0; i < items.length(); i++) {
      JSONObject it = items.getJSONObject(i);
      String fecha = it.optString("f", "");
      String plan = it.optString("p", "");
      String marca;
      if (!fecha.isEmpty() && fecha.compareTo(hoy) < 0) {
        vencidas++;
        marca = "Vencida: ";
      } else if (fecha.equals(hoy)) {
        vencenHoy++;
        marca = "Vence hoy: ";
      } else if (!plan.isEmpty() && plan.compareTo(hoy) <= 0) {
        tocan++;
        marca = "";
      } else {
        continue;
      }
      lineas.add(marca + it.optString("t", "") + " · " + it.optString("a", ""));
    }
    int total = vencidas + vencenHoy + tocan;
    if (total == 0) return;

    NotificationManagerCompat gestor = NotificationManagerCompat.from(context);
    if (!gestor.areNotificationsEnabled()) return;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel canal = new NotificationChannel(CANAL, "Entregas y plan del día",
          NotificationManager.IMPORTANCE_DEFAULT);
      NotificationManager sistema = context.getSystemService(NotificationManager.class);
      if (sistema != null) sistema.createNotificationChannel(canal);
    }

    List<String> partes = new ArrayList<>();
    if (vencidas > 0) partes.add(vencidas + (vencidas == 1 ? " vencida" : " vencidas"));
    if (vencenHoy > 0) partes.add(vencenHoy + (vencenHoy == 1 ? " vence hoy" : " vencen hoy"));
    if (tocan > 0) partes.add(tocan + " del plan");
    StringBuilder resumen = new StringBuilder();
    for (String parte : partes) {
      if (resumen.length() > 0) resumen.append(" · ");
      resumen.append(parte);
    }

    NotificationCompat.InboxStyle estilo = new NotificationCompat.InboxStyle();
    for (int i = 0; i < Math.min(lineas.size(), 6); i++) estilo.addLine(lineas.get(i));
    if (lineas.size() > 6) estilo.setSummaryText("y " + (lineas.size() - 6) + " más");

    PendingIntent abrir = PendingIntent.getActivity(context, 0,
        new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    NotificationCompat.Builder aviso = new NotificationCompat.Builder(context, CANAL)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle(total == 1 ? "Hoy tienes 1 actividad" : "Hoy tienes " + total + " actividades")
        .setContentText(resumen.toString())
        .setStyle(estilo)
        .setColor(0xFF1F3A5F)
        .setAutoCancel(true)
        .setPriority(vencidas > 0 ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(abrir);
    try {
      gestor.notify(ID_AVISO, aviso.build());
    } catch (SecurityException sinPermiso) {
      // El usuario no dio permiso de notificaciones: no hay nada que mostrar.
    }
  }
}
