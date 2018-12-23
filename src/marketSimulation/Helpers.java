package marketSimulation;

public class Helpers {

    public static Constants.FoodType getRandomFoodType() {
        return Constants.FoodType.values()[(int) (Math.random() * 10) % Constants.FoodType.values().length];
    }

    public static Constants.BuyingStrategy getRandomBuyingStrategy() {
        return Constants.BuyingStrategy.values()[(int) (Math.random() * 10) % Constants.BuyingStrategy.values().length];
    }

    public static int getRandomNumberBetweenOneAndFive() {
        return ((int) (Math.random() * 10)) % 5 + 1;
    }

    public static int getRandomNumberBetweenThreeAndFive() {
        return ((int) (Math.random() * 10)) % 3 + 3;
    }

    public static int getRandomNumberBetweenOneAndTen() {
        return ((int) (Math.random() * 10)) + 1;
    }

    public static int getRandomNumberBetweenOneAndOneHundred() {
        return ((int) (Math.random() * 100)) + 1;
    }
}
