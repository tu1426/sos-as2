package marketSimulation;

import java.io.Serializable;

public class FoodResponse implements Serializable {

    private Constants.FoodType foodType;
    private int price;
    private int quality;

    private FoodResponse() {}

    public FoodResponse(Constants.FoodType foodType, int price, int quality) {
        this.foodType = foodType;
        this.price = price;
        this.quality = quality;
    }

    public Constants.FoodType getFoodType() {
        return foodType;
    }

    public void setFoodType(Constants.FoodType foodType) {
        this.foodType = foodType;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }
}
