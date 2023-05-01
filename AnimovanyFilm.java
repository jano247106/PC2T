import java.util.ArrayList;

public class AnimovanyFilm extends Film{
    public int age;
    public ArrayList<Animator> zoznamAnimatorov;

    public AnimovanyFilm(int id, String nazov, String reziser, int year) {
        super(id, nazov, reziser, year);
        this.zoznamAnimatorov = new ArrayList<>();
    }
    public AnimovanyFilm(int id, String nazov, String reziser, int year, int age){
        super(id, nazov, reziser, year);
        this.age = age;
        this.zoznamAnimatorov = new ArrayList<>();
    }

    public String toString(){
        return String.format("%s\t %s\t %d\t %.2f\t %d\t",this.nazov, this.reziser, this.year, this.getPriemerneHodnotenie(), this.age);
    }

    @Override
    public String celyFilm(){
        StringBuilder film = new StringBuilder();
        film.append(this.toString());
        for(Animator animator: this.zoznamAnimatorov){
            film.append(animator);
            film.append(", ");
        }
        return film.toString();
    }

}
