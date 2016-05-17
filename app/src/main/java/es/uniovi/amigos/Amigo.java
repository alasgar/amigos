package es.uniovi.amigos;



public class Amigo {
    private String name;
    private double longitud;
    private double latitud;

    public Amigo (String valorNombre, double valorLongitud, double valorLatitud) {
        name = valorNombre;
        longitud = valorLongitud;
        latitud = valorLatitud;

    }
    public void setName (String valorName) {
        this.name = valorName;
    }
    public String getName () {
        return name;
    }
    public void setLongitud (double valorLongitud) {
        this.longitud = valorLongitud;
    }
    public double getLongitud () {
        return longitud;
    }
    public void setLatitud (double valorLatitud) {
        this.latitud = valorLatitud;
    }
    public double getLatitud () {
        return latitud;
    }
}
