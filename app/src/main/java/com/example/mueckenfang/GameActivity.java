package com.example.mueckenfang;

import android.app.Dialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Date;
import java.util.Random;

import androidx.appcompat.app.AppCompatActivity;


public class GameActivity extends AppCompatActivity implements View.OnClickListener, Runnable {
    private static final String ELEFANT = "ELEFANT";
    private static final long HOECHSTALTER_MS = 2000;
    public static final int DELAY_MILLIS = 1000;
    public static final int ZEITSCHEIBEN = 60;
    private int punkte;
    private int runde;
    private int gefangeneMuecken;
    private int zeit;
    private float massstab;
    private int muecken;
    private Random zufallsgenerator = new Random();
    private ViewGroup spielbereich;
    private boolean spielLaeuft;
    private Handler handler = new Handler();
    private MediaPlayer mp;
    private MediaPlayer mpGameOver; // Neue MediaPlayer-Instanz für Game Over


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);
        massstab = getResources().getDisplayMetrics().density;
        spielbereich = findViewById(R.id.spielbereich);
        mp = MediaPlayer.create(this, R.raw.summen);
        mpGameOver = MediaPlayer.create(this, R.raw.zonk); // Initialisiere den Game Over Sound
        spielStarten();
    }

    private void spielStarten() {
        spielLaeuft = true;
        runde = 0;
        punkte = 0;
        starteRunde();
    }

    private void bildschirmAktualisieren() {
        TextView tvPunkte = findViewById(R.id.points);
        tvPunkte.setText(Integer.toString(punkte));
        TextView tvRunde = findViewById(R.id.round);
        tvRunde.setText(Integer.toString(runde));
        TextView tvTreffer = findViewById(R.id.hits);
        tvTreffer.setText(Integer.toString(gefangeneMuecken));
        TextView tvZeit = findViewById(R.id.time);
        tvZeit.setText(Integer.toString(zeit / (1000 / DELAY_MILLIS)));
        FrameLayout flTreffer = findViewById(R.id.bar_hits);
        FrameLayout flZeit = findViewById(R.id.bar_time);
        ViewGroup.LayoutParams lpTreffer = flTreffer.getLayoutParams();
        lpTreffer.width = muecken > 0 ? Math.round(massstab * 300 *
                Math.min(gefangeneMuecken, muecken) / muecken) : 0;
        ViewGroup.LayoutParams lpZeit = flZeit.getLayoutParams();
        lpZeit.width = Math.round(massstab * zeit * 300 / ZEITSCHEIBEN);

    }

    private void zeitHerunterzaehlen() {
        zeit = zeit - 1;
        if (zeit % (1000 / DELAY_MILLIS) == 0) {
            float zufallszahl = zufallsgenerator.nextFloat();
            double wahrscheinlichkeit = muecken * 1.5 / 60;

            if (wahrscheinlichkeit > 1) {
                eineMueckeAnzeigen();
                if (zufallszahl < wahrscheinlichkeit - 1) {
                    eineMueckeAnzeigen();
                }
            } else {
                if (zufallszahl < wahrscheinlichkeit) {
                    eineMueckeAnzeigen();
                }
            }
        }
        mueckenVerschwinden();
        bildschirmAktualisieren();
        if (!pruefeSpielende()) {
            if (!pruefeRundenende()) {
                handler.postDelayed(this, DELAY_MILLIS);
            }
        }
    }

    private boolean pruefeRundenende() {
        if (gefangeneMuecken >= muecken) {
            starteRunde();
            return true;
        }
        return false;
    }

    private void starteRunde() {
        runde = runde + 1;
        muecken = runde * 20;
        gefangeneMuecken = 0;
        zeit = ZEITSCHEIBEN;
        bildschirmAktualisieren();
        handler.postDelayed(this, 1000);
        int id = getResources().getIdentifier("hintergrund" + runde, "drawable", this.getPackageName());
        if (id > 0) {
            LinearLayout l = findViewById(R.id.hintergrund);
            l.setBackgroundResource(id);
        }
    }

    private boolean pruefeSpielende() {
        if (zeit == 0 && gefangeneMuecken < muecken) {
            gameOver();
            return true;
        }
        return false;
    }

    private void gameOver() {
        if (mpGameOver != null) {
            mpGameOver.start();
        }
        Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.gameover);
        dialog.show();
        spielLaeuft = false;

    }

    private void mueckenVerschwinden() {
        int nummer = 0;
        while (nummer < spielbereich.getChildCount()) {
            ImageView muecke = (ImageView) spielbereich.getChildAt(nummer);
            Date geburtsdatum = (Date) muecke.getTag(R.id.geburtsdatum);
            long alter = (new Date()).getTime() - geburtsdatum.getTime();
            if (alter > HOECHSTALTER_MS) {
                spielbereich.removeView(muecke);
            } else {
                nummer++;
            }
        }
    }

    private void eineMueckeAnzeigen() {
        int breite = spielbereich.getWidth();
        int hoehe = spielbereich.getHeight();
        int muecke_breite = Math.round(massstab * 50);
        int muecke_hoehe = Math.round(massstab * 42);
        int links = zufallsgenerator.nextInt(breite - muecke_breite);
        int oben = zufallsgenerator.nextInt(hoehe - muecke_hoehe);

        ImageView muecke = new ImageView(this);
        muecke.setImageResource(R.drawable.muecke);
        muecke.setOnClickListener(this);
        muecke.setTag(R.id.geburtsdatum, new Date());

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(muecke_breite, muecke_hoehe);
        params.leftMargin = links;
        params.topMargin = oben;
        params.gravity = Gravity.TOP + Gravity.LEFT;

        spielbereich.addView(muecke, params);
        if (zufallsgenerator.nextFloat() < 0.05) {
            muecke.setImageResource(R.drawable.elefant);
            muecke.setTag(R.id.tier, ELEFANT);
        } else {
            muecke.setImageResource(R.drawable.muecke);
        }

        mp.seekTo(0);
        mp.setVolume(0.5f, 0.5f);
        mp.start();
    }

    @Override
    protected void onDestroy() {
        // Freigeben der MediaPlayer-Ressourcen
        if (mp != null) {
            mp.release();
        }
        if (mpGameOver != null) {
            mpGameOver.release();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View muecke) {
        if (muecke.getTag(R.id.tier) == ELEFANT) {
            punkte -= 1000;
        } else {
            gefangeneMuecken++;
            punkte += 100;
        }
        bildschirmAktualisieren();
        spielbereich.removeView(muecke);
        mp.pause();
    }

    @Override
    public void run() {
        zeitHerunterzaehlen();
    }
}

/**
 * Kapitel 6.3 weiter machen auf Seite 257
 */