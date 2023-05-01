import java.util.ArrayList;

public class HranyFilm extends Film{
    public ArrayList<Herec> zoznamHercov;


    public HranyFilm(int id, String name, String reziser, int year) {
        super(id, name, reziser, year);
        this.zoznamHercov = new ArrayList<>();
    }

    public String toString(){
        return String.format("%s\t %s\t %d\t %.2f\t",this.nazov, this.reziser, this.year, this.getPriemerneHodnotenie());
    }
    @Override
    public String celyFilm(){
        StringBuilder film = new StringBuilder();
        film.append(this.toString());
        for(Herec herec: this.zoznamHercov){
            film.append(herec);
            film.append(", ");
        }
        return film.toString();
    }


}
