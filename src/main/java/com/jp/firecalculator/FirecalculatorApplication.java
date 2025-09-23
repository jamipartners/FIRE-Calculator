package com.jp.firecalculator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FirecalculatorApplication {

	int currentAge=29;
	int age = 26;
	int retirementAge=60;
	int initialInvestment=500000;
	double annualIncrement=0.174410441428737;
	int monthlyExpenses=200000;
	double inflation=0.08;
	int year;
	double monthly = 20000;


	 // Expected returns
    double equityReturn = 0.1724; // 17.24%
    double debtReturn = 0.0987;   // 9.87%
    double moneyMarketReturn = 0.0956; // 9.56%

	double expectedReturn;

      private static final Map<String, Allocation> riskAllocations = new HashMap<>();
	  private static final Map<String,LifeStyle> lifeStyle=new HashMap<>();

     static {
        riskAllocations.put("high", new Allocation(0.80, 0.20, 0.00));
        riskAllocations.put("medium", new Allocation(0.50, 0.40, 0.10));
        riskAllocations.put("low", new Allocation(0.25, 0.60, 0.15));
		lifeStyle.put("luxury",new LifeStyle(37.5));
		lifeStyle.put("comfortable",new LifeStyle(25));
		lifeStyle.put("modest",new LifeStyle(18.5));
    }

	public static Allocation getAllocation(String riskLevel) {
        return riskAllocations.getOrDefault(riskLevel.trim().toLowerCase(),
                new Allocation(0.0, 0.0, 0.0));
    }

	public static LifeStyle getLifeStyle(String style){

		return lifeStyle.getOrDefault(style.trim().toLowerCase(),new LifeStyle(0));
	}

    Allocation high = getAllocation("high");
	static LifeStyle luxury=getLifeStyle("Luxury");

	public double getExpectedReturn() {

		double equityComponent=high.equity*equityReturn;
		double debtComponent=high.debt*debtReturn;
		double moneyMarketComponent=high.moneymarktet*moneyMarketReturn;
		return equityComponent+debtComponent+moneyMarketComponent;
	}
	
	 public static double calculateMonthlyContribution(int year, double baseMonthly, double previousMonthly, double annualIncrement) {
        double currentMonthly;

        if (year == 1) {
            currentMonthly = baseMonthly;
        } else {
            currentMonthly = previousMonthly * (1 + annualIncrement);
        }
        return currentMonthly;
    }


	public static double annualContribution(int currentAge,int retirementAge,double monthly,int year,int initialInvestment){

		try {
            if ((currentAge + 1) > retirementAge) {
                return 0;
            } else {

                if (year == 1){
                    return (monthly * 12) + initialInvestment;
                } else {
                    return (monthly * 12);
                }
            }
        } catch (Exception e) {
            return 0;
        }
	}

	public static double cummulativeContribution(int age,int retirementAge,double annual,double previousCumulative,double monthly,double initialInvestment,int year){

		 try {
            if (age > retirementAge) {
                return 0;
            } else {
                if (year == 1) {
                    return (monthly * 12) + initialInvestment;
                } else {
                    return previousCumulative + annual;
                }
            }
        } catch (Exception e) {
            return 0;
        }

	}

	public double profit(){

		return 0;
	}
 public static double portfolio(double expectedReturn, int year, double initialInvestment, double currentMonthly, double previousClosingBalance) {
        try {
            double part1, part2;

            if (year == 1) {
                part1 = fv(expectedReturn, 1, 0, -initialInvestment, true);
                part2 = fv(expectedReturn / 12.0, 12, -currentMonthly, 0, true);

                double result = part1 + part2;
                return result;
            } else {
                part1 = fv(expectedReturn, 1, 0, -previousClosingBalance, true);
                part2 = fv(expectedReturn / 12, 12, -currentMonthly, 0, true);

                double result = part1 + part2;
                return result;
            }
        } catch (Exception e) {
            return 0;
        }
		
    }

	public static double inflationAdjustedBalance(int monthlyExpenses,int year,int age,int retirementAge,double previousInflationAdjustedBalance,double inflation){

        try{

            if(age+1>retirementAge){
                return 0;
            }
            else{

                
        if(year==1){
            return monthlyExpenses*12*inflation+monthlyExpenses*12;
        }
        else{
            return previousInflationAdjustedBalance*inflation+previousInflationAdjustedBalance;
        }
            }
        }
        catch(Exception e){
            return 0;
        }

	}

	public static double fireAmountTarget(double inflationAdjustedBalance){

		return inflationAdjustedBalance*luxury.rate;
	}

	public static double fv(double rate, int nper, double pmt, double pv, boolean type) {
    MathContext mc = new MathContext(15, RoundingMode.HALF_UP);

    BigDecimal bdRate = new BigDecimal(rate, mc);
    BigDecimal bdPv   = new BigDecimal(pv, mc);
    BigDecimal bdPmt  = new BigDecimal(pmt, mc);

    if (rate == 0) {
        return -(bdPv.add(bdPmt.multiply(new BigDecimal(nper), mc))).doubleValue();
    } else {
        BigDecimal r1   = BigDecimal.ONE.add(bdRate, mc).round(mc);
        BigDecimal pow  = r1.pow(nper, mc).round(mc);
        BigDecimal factor = pow.subtract(BigDecimal.ONE, mc)
                .divide(bdRate, mc)
                .round(mc);

        if (type) {
            factor = factor.multiply(r1, mc).round(mc);
        }

        BigDecimal result = bdPv.negate(mc).multiply(pow, mc)
                .subtract(bdPmt.multiply(factor, mc), mc)
                .round(mc);

        return result.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}


	public static void main(String[] args) {
		SpringApplication.run(FirecalculatorApplication.class, args);

		FirecalculatorApplication app=new FirecalculatorApplication();
		app.expectedReturn = app.getExpectedReturn();



		
        double previousCumulative = 0;
        double previousClosingBalance = 0;
        double previousInflationAdjustedBalance=0;
        double previousMonthly = app.monthly;

        // Loop through each year from current age to retirement age
        for (int loopAge = app.currentAge; loopAge < app.retirementAge; loopAge++) {
            // Update age and year for current iteration
            app.age = loopAge + 1;
            app.year = app.age - app.currentAge;

            // Perform calculations
			double currentMonthly =calculateMonthlyContribution(app.year,app.monthly,previousMonthly,app.annualIncrement);
			previousMonthly = currentMonthly;

            double annual = annualContribution(loopAge,app.retirementAge,currentMonthly,app.year,app.initialInvestment);


            double cumulative=cummulativeContribution(loopAge,app.retirementAge,annual,previousCumulative,app.monthly,app.initialInvestment,app.year);

            double closingBalance =portfolio(app.expectedReturn, app.year, app.initialInvestment, currentMonthly, previousClosingBalance);
            double profit = closingBalance - cumulative;
            double inflationAdjusted=inflationAdjustedBalance(app.monthlyExpenses,app.year,loopAge,app.retirementAge,previousInflationAdjustedBalance,app.inflation);
            double fireamount=fireAmountTarget(inflationAdjusted);
		

            System.out.println("Age " + app.age +
                    " | Year " + app.year +
                    " | Monthly " + Math.round(currentMonthly)+
					" | Annual " + Math.round(annual)+
					" | Cumulative " + Math.round(cumulative)+
                    " | Portfolio "  +Math.round(closingBalance)+
                    " | Profit:"+Math.round(profit)+
                    " | Inflation Adjusted Balance ::"+Math.round(inflationAdjusted)+
                    " | Fire Amount Target ::"+Math.round(fireamount)
					);
            System.out.println("-------------------------------------------------------------------------------------------------------------------------------------------");
            // Update previous values for next iteration
            previousCumulative = cumulative;
            previousClosingBalance = closingBalance;
            previousMonthly = currentMonthly;
            previousInflationAdjustedBalance=inflationAdjusted;

	}

}
}
