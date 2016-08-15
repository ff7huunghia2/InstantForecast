package com.example.dell.instantforecast;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Dell on 8/13/2016.
 */
public class WeatherInfoFragment extends Fragment {

    static TextView detailsField, currentTemperatureField, max_temperature, min_temperature, weatherIcon;
    static ImageView max_img, min_img;
    static Typeface weatherFont;
    static ScrollView mainScrollView;
    static int screenHeight;
    static ArrayList<Bitmap> blurred_background_image;
    static Bitmap background_image;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.weather_info_fragment, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        weatherFont = Typeface.createFromAsset(MainActivity.mainActivity.getAssets(), "fonts/weathericons-regular-webfont.ttf");
        detailsField = (TextView) view.findViewById(R.id.details_field);
        currentTemperatureField = (TextView) view.findViewById(R.id.current_temperature_field);
        weatherIcon = (TextView) view.findViewById(R.id.weather_icon);
        weatherIcon.setTypeface(weatherFont);

        max_img = (ImageView) view.findViewById(R.id.max_icon);
        max_temperature = (TextView) view.findViewById(R.id.max_temperature);
        min_img = (ImageView) view.findViewById(R.id.min_icon);
        min_temperature = (TextView) view.findViewById(R.id.min_temperature);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        MainActivity.mainActivity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        screenHeight = displaymetrics.heightPixels;



        blurred_background_image = new ArrayList<>();

        mainScrollView = (ScrollView) view.findViewById(R.id.weather_info_scroll_view);
        mainScrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {

            @Override
            public void onScrollChanged() {
                int scrollY = mainScrollView.getScrollY(); //for verticalScrollView
                int stepScreenHeight = screenHeight/3;
                //DO SOMETHING WITH THE SCROLL COORDINATES
                //System.out.println(scrollY + " + " + isScrolled);
                if(scrollY <= 0){
                    background_image = BitmapFactory.decodeResource(MainActivity.mainActivity.getResources(), R.drawable.back);
                    MainActivity.mainActivity.background_image_view.setImageBitmap(background_image);
                }
                if(scrollY > stepScreenHeight){
                    int y = screenHeight / 5;
                    if(scrollY >= stepScreenHeight && scrollY < (stepScreenHeight + y)){
                        MainActivity.mainActivity.background_image_view.setImageBitmap(blurred_background_image.get(0));
                    }
                    if(scrollY >= (stepScreenHeight + y) && scrollY < (stepScreenHeight + 2*y)){
                        MainActivity.mainActivity.background_image_view.setImageBitmap(blurred_background_image.get(1));
                    }
                    if(scrollY >= (stepScreenHeight + 2*y) && scrollY < (stepScreenHeight + 3*y)){
                        MainActivity.mainActivity.background_image_view.setImageBitmap(blurred_background_image.get(2));
                    }
                }
            }
        });
    }

    static public void loadWeatherInfo(final String Lat, final String Lon, final boolean doAddCity, final boolean doAddCurrentLocation) {
        if (isOnline()) {
            OpenWeatherMapApiManager.placeIdTask getCurrentWeatherTask = new OpenWeatherMapApiManager.placeIdTask(new OpenWeatherMapApiManager.AsyncResponse() {
                public void processFinish(String weather_country, String weather_city, String weather_description, String weather_temperature, String weather_humidity, String weather_pressure, String weather_updatedOn, String weather_iconText, String sun_rise) {
                    CityNowWeatherInfo current_cityNowWeatherInfo = new CityNowWeatherInfo(
                            weather_city,
                            weather_country,
                            weather_iconText,
                            weather_temperature,
                            Lat,
                            Lon
                    );
                    if (doAddCity) {
                        if (doAddCurrentLocation) {
                            MainActivity.appDataModel.current_city = current_cityNowWeatherInfo;
                        } else {
                            boolean cityExisted = false;
                            for(int i = 0; i < MainActivity.appDataModel.city_list.size(); i++){
                                if(current_cityNowWeatherInfo.name.equals(MainActivity.appDataModel.city_list.get(i).name)){
                                    cityExisted = true;
                                    break;
                                }
                            }
                            if(!cityExisted){
                                MainActivity.appDataModel.city_list.add(current_cityNowWeatherInfo);
                            }
                            else{// Update City Info

                            }
                            MainActivity.mainActivity.updateNavigationMenu();
                        }
                    }
                    MainActivity.city_name_textview.setText(weather_city + "," + weather_country);
                    detailsField.setText(weather_description);
                    currentTemperatureField.setText(weather_temperature);
                    //humidity_field.setText("Humidity: "+weather_humidity);
                    //pressure_field.setText("Pressure: "+weather_pressure);
                    weatherIcon.setText(Html.fromHtml(weather_iconText));
                    max_img.setImageResource(R.drawable.ic_vertical_align_top_white_24dp);
                    min_img.setImageResource(R.drawable.ic_vertical_align_bottom_white_24dp);
                    max_temperature.setText("30°");
                    min_temperature.setText("24°");

                    blurred_background_image.clear();
                    Bitmap bitmap = BitmapFactory.decodeResource(MainActivity.mainActivity.getResources(), R.drawable.back);
                    blurred_background_image.add(blur(bitmap,5f));
                    bitmap = BitmapFactory.decodeResource(MainActivity.mainActivity.getResources(), R.drawable.back);
                    blurred_background_image.add(blur(bitmap,15f));
                    bitmap = BitmapFactory.decodeResource(MainActivity.mainActivity.getResources(), R.drawable.back);
                    blurred_background_image.add(blur(bitmap,25f));
                }
            });
            getCurrentWeatherTask.execute(Lat, Lon);

            GoogleTimezoneAPI.getDateTimeByLocationTask getDateTimeTask = new GoogleTimezoneAPI.getDateTimeByLocationTask(new GoogleTimezoneAPI.AsyncResponse() {
                @Override
                public void processFinish(String date) {
                    MainActivity.city_time_textview.setText(date);
                }
            });
            getDateTimeTask.execute(Lat, Lon);
        } else {
            Toast networkError = Toast.makeText(MainActivity.mainActivity, "Can't connect to internet!!", Toast.LENGTH_LONG);
            networkError.show();
        }
    }

    static public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) MainActivity.mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    static public Bitmap blur(Bitmap image,float radius) {
        if (null == image) return null;

        Bitmap outputBitmap = Bitmap.createBitmap(image);
        final RenderScript renderScript = RenderScript.create(MainActivity.mainActivity);
        Allocation tmpIn = Allocation.createFromBitmap(renderScript, image);
        Allocation tmpOut = Allocation.createFromBitmap(renderScript, outputBitmap);

        //Intrinsic Gausian blur filter
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        theIntrinsic.setRadius(radius);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);
        return outputBitmap;
    }
}