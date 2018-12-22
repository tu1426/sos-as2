package marketSimulation;

import java.io.Serializable;

public class FoodRequest implements Serializable {

    private Constants.FoodType foodType;
    private int maxPrice;
    private int minQuality;
    private Constants.BuyingStrategy buyingStrategy;

    private FoodRequest() {}

    public FoodRequest(Constants.FoodType foodType, int maxPrice, int minQuality, Constants.BuyingStrategy buyingStrategy) {
        this.foodType = foodType;
        this.maxPrice = maxPrice;
        this.minQuality = minQuality;
        this.buyingStrategy = buyingStrategy;
    }

    public Constants.FoodType getFoodType() {
        return foodType;
    }

    public void setFoodType(Constants.FoodType foodType) {
        this.foodType = foodType;
    }

    public int getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(int maxPrice) {
        this.maxPrice = maxPrice;
    }

    public int getMinQuality() {
        return minQuality;
    }

    public void setMinQuality(int minQuality) {
        this.minQuality = minQuality;
    }

    public Constants.BuyingStrategy getBuyingStrategy() {
        return buyingStrategy;
    }

    public void setBuyingStrategy(Constants.BuyingStrategy buyingStrategy) {
        this.buyingStrategy = buyingStrategy;
    }
}
