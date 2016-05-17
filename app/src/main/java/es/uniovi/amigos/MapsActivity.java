package es.uniovi.amigos;

import android.content.Context;
import android.content.DialogInterface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    String LIST_URL = "http://amigosgrupodoceplcuatro2.azurewebsites.net/api/amigo";
    int UPDATE_PERIOD =6000;
    List<Amigo> lista = new ArrayList<Amigo>();
    String mUserName = null;
    JSONObject jsonAmigoNuevo = null;
    JSONObject jsonAmigoActual = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        askUserName();

        Timer timer = new Timer();
        TimerTask updateAmigos = new UpdateAmigoPosition();
        timer.scheduleAtFixedRate(updateAmigos, 0, UPDATE_PERIOD);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    private void mostrarLista(List<Amigo> lista) {
        if(lista!=null) {
            mMap.clear();
            for (Amigo amigo : lista) {
                LatLng colocacion = new LatLng(amigo.getLatitud(), amigo.getLongitud());
                mMap.addMarker(new MarkerOptions().position(colocacion).title(amigo.getName()));
            }
        }
       // mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(42.665068, -3.985442)));
    }

    public void askUserName() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Ajustes");
        alert.setMessage("Nombre de Usuario:");

        // Crear un EditText para obtener el nombre
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mUserName = input.getText().toString();
                resgistroListenerPosition();
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    public void resgistroListenerPosition(){
        // Se debe adquirir una referencia al Location Manager del sistema
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Se obtiene el mejor provider de posición
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        String  provider = locationManager.getBestProvider(criteria, true);

        // Se crea un listener de la clase que se va a definir luego
        MyLocationListener locationListener = new MyLocationListener();

        // Se registra el listener con el Location Manager para recibir actualizaciones
        locationManager.requestLocationUpdates(provider, 2000, 10, locationListener);

        // Comprobar si se puede obtener la posición ahora mismo
        Location location = locationManager.getLastKnownLocation(provider);
        if (location == null) {
            provider = locationManager.NETWORK_PROVIDER;
            locationManager.requestLocationUpdates(provider,2000, 10, locationListener);
        }
    }

    class UpdateAmigoPosition extends TimerTask {
        public void run() {
            new UpdateListTask().execute(LIST_URL);
        }
    }

    class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            // Se llama cuando hay una nueva posición para ese location provider
            double lati = location.getLatitude();
            double longi = location.getLongitude();

            jsonAmigoNuevo = new JSONObject();
            try {
                jsonAmigoNuevo.put("name",mUserName);
                jsonAmigoNuevo.put("longi", longi);
                jsonAmigoNuevo.put("lati",lati);
            }catch (JSONException e) {
                e.printStackTrace();
            }
            new ModificaAmigoTask().execute(LIST_URL);

        }

        // Se llama cuando cambia el estado
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        // Se llama cuando se activa el provider
        @Override
        public void onProviderEnabled(String provider) {}

        // Se llama cuando se desactiva el provider
        @Override
        public void onProviderDisabled(String provider) {}
    }

    public class UpdateListTask extends AsyncTask<String, Void, List<Amigo> >{

        @Override
        protected void onPostExecute(List<Amigo> result) {
            lista = result;
            mostrarLista(lista);
        }

        @Override
        protected List<Amigo> doInBackground(String... urls) {
            // urls es un array de elementos. El primero serÃ¡ urls[0]
            try {
                return getCurrencyRateUsdRate(urls[0]);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e){
                e.printStackTrace();
            }
            return null;
        }
        protected List<Amigo> getCurrencyRateUsdRate(String urlString) throws IOException, JSONException {
            InputStream datos = openUrl(urlString);
            String lineaDatos = readStream(datos);
            List<Amigo> listaAmigos =parseDataFromNetwork(lineaDatos);
            return listaAmigos;
        }
        protected InputStream openUrl(String urlString) throws IOException {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            // Starts the query
            conn.connect();
            return conn.getInputStream();
        }
        protected String readStream(InputStream urlStream) throws IOException {
            BufferedReader r = new BufferedReader(new InputStreamReader(urlStream));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            return total.toString();
        }
        private List<Amigo> parseDataFromNetwork(String data) throws IOException, JSONException {

            List<Amigo> amigosList = new ArrayList<Amigo>();

            JSONArray amigos = new JSONArray(data);

            for(int i = 0; i < amigos.length(); i++) {
                JSONObject amigoObject = amigos.getJSONObject(i);

                String name = amigoObject.getString("name");
                String longi = amigoObject.getString("longi");
                String lati = amigoObject.getString("lati");

                longi = longi.replace(",",".");
                lati = lati.replace(",",".");

                double longiNumber;
                double latiNumber;
                try {
                    longiNumber = Double.parseDouble(longi);
                    latiNumber = Double.parseDouble(lati);
                } catch (NumberFormatException nfe) {
                    continue;
                }

                amigosList.add(new Amigo(name, longiNumber, latiNumber));
            }
            return amigosList;
        }
    }

    public class ModificaAmigoTask extends AsyncTask<String, Void, Integer >{

        @Override
        protected void onPostExecute(Integer result) {
            if(result!=204) {
                Toast.makeText(getApplicationContext(), "Error en el nombre", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected Integer doInBackground(String... urls) {
            // urls es un array de elementos. El primero serÃ¡ urls[0]
            try {
                return openUrl(urls[0]);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e){
                e.printStackTrace();
            }
            return 0;
        }

        protected int openUrl(String urlString) throws IOException, JSONException{
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("PUT");
            conn.setDoInput(true);

            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            String payload = jsonAmigoNuevo.toString();

            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
            osw.write(payload);
            osw.flush();
            osw.close();

            // Starts the query
            conn.connect();
            return conn.getResponseCode();
        }
    }
}
