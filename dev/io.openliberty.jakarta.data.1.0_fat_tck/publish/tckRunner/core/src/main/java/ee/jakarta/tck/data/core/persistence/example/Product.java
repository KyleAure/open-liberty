package ee.jakarta.tck.data.core.persistence.example;

import ee.jakarta.tck.data.core.persistence.example.Product;
import io.openliberty.data.entity.Entity;
import io.openliberty.data.entity.Id;
import io.openliberty.data.entity.Transient;

@Entity
public class Product {

    @Id
    private Long id;
    
    private String name;
    private Double price;
    
    @Transient
    private Double surgePrice;
    
    public static Product of(Long id, String name, Double price, Double surgePrice) {
        return new Product(id, name, price, surgePrice);
    }

    public Product(Long id, String name, Double price, Double surgePrice) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.surgePrice = surgePrice;
    }

    public Product() {
        //do nothing
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getSurgePrice() {
        return surgePrice;
    }

    public void setSurgePrice(Double surgePrice) {
        this.surgePrice = surgePrice;
    }
    
}