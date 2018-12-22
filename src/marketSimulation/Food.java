package marketSimulation;

public class Food {

    private Constants.FoodType foodType;
    private int price;		// 1-10
    private int quality;	// 1-5

    public Food() {
        this.foodType = Constants.FoodType.values()[(int) (Math.random() * 10) % 4];
        this.price = ((int) (Math.random() * 10)) % 10 + 1;

        if(this.price == 1 || this.price == 2) {
            this.quality = 5;
        } else if(this.price == 3 || this.price == 4) {
            this.quality = 4;
        } else if(this.price == 5 || this.price == 6) {
            this.quality = 3;
        } else if(this.price == 7 || this.price == 8) {
            this.quality = 2;
        } if(this.price == 9 || this.price == 10) {
            this.quality = 1;
        }
    }

    public Food(Constants.FoodType foodType, int price, int quality) {
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