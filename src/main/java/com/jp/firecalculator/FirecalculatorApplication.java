package com.jp.firecalculator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FirecalculatorApplication {

    // ================= INPUTS / INSTANCE VARIABLES =================

    // User's current age
    int currentAge = 29;

    // Age variable used in loops / calculations (starts at 26 here, but updated
    int age = 26;

    // Age at which the user plans to retirement
    int retirementAge = 60;

    // Initial lump sum investment amount at the start of calculation
    int initialInvestment = 500000;

    // Annual increment rate for monthly contributions (as a decimal, e.g., 17.44%)
    double annualIncrement = 0.174410441428737;

    // User's expected monthly expenses (used for FIRE / retirement target)
    int monthlyExpenses = 200000;

    // Expected annual inflation rate (used to adjust expenses over time)
    double inflation = 0.08;

    // Variable to store the current year in the working life loop
    int year;

    // Base monthly contribution amount (used to calculate yearly contributions)
    double monthly = 20000;

    // User's expected lifespan (used for redemption / post-retirement calculations)
    int lifeExpectancy = 100;

    // Year count for redemption (post-retirement) calculations
    int redempYear;

    // Age during redemption (post-retirement) calculations
    int redAge;

    // ================= EXPECTED RETURNS & RISK / LIFESTYLE ALLOCATIONS
    // =================

    // Fixed expected annual returns for different asset classes
    double equityReturn = 0.1724; // Equity expected return = 17.24%
    double debtReturn = 0.0987; // Debt expected return = 9.87%
    double moneyMarketReturn = 0.0956; // Money Market expected return = 9.56%

    // Calculated expected return based on user's selected risk allocation
    // (high/medium/low)
    double expectedReturn;

    // ================= RISK ALLOCATIONS =================
    // Mapping risk levels to allocation percentages for each asset type
    private static final Map<String, Allocation> riskAllocations = new HashMap<>();

    // Mapping lifestyle choices to a FIRE multiplier rate
    private static final Map<String, LifeStyle> lifeStyle = new HashMap<>();

    // Static block to initialize risk allocation and lifestyle mappings
    static {
        // High risk: 80% equity, 20% debt, 0% money market
        riskAllocations.put("high", new Allocation(0.80, 0.20, 0.00));

        // Medium risk: 50% equity, 40% debt, 10% money market
        riskAllocations.put("medium", new Allocation(0.50, 0.40, 0.10));

        // Low risk: 25% equity, 60% debt, 15% money market
        riskAllocations.put("low", new Allocation(0.25, 0.60, 0.15));

      //  riskAllocations.put("lower", new Allocation(0.25, 0.60, 0.15));
      //  riskAllocations.put("VAS Debt", new Allocation(0.25, 0.60, 0.15));

        // Lifestyle options affecting FIRE multiplier
        lifeStyle.put("luxury", new LifeStyle("Luxury", 37.5)); // FIRE target = 37.5x annual expenses
        lifeStyle.put("comfortable", new LifeStyle("Comfortable", 25)); // FIRE target = 25x annual expenses
        lifeStyle.put("modest", new LifeStyle("Modest", 18.5)); // FIRE target = 18.5x annual expenses
    }

    // ================= HELPER METHODS TO FETCH ALLOCATION & LIFESTYLE
    // =================

    /**
     * Returns the portfolio allocation for a given risk level.
     * 
     * @param riskLevel - "high", "medium", or "low"
     * @return Allocation object containing equity, debt, and money market
     *         percentages
     * 
     *         If the riskLevel is not found, returns 0% for all asset classes.
     */
    public static Allocation getAllocation(String riskLevel) {
        // Trim whitespace and convert to lowercase for consistency
        return riskAllocations.getOrDefault(
                riskLevel.trim().toLowerCase(),
                new Allocation(0.0, 0.0, 0.0) // Default if invalid riskLevel
        );
    }

    /**
     * Returns the lifestyle FIRE multiplier for a given style.
     * 
     * @param style - "luxury", "comfortable", or "modest"
     * @return LifeStyle object containing the FIRE multiplier rate
     * 
     *         If the style is not found, returns 0 (no multiplier).
     */
    public static LifeStyle getLifeStyle(String style) {
        return lifeStyle.getOrDefault(
                style.trim().toLowerCase(),
                new LifeStyle("", 0) // Default if invalid style
        );
    }

    // Fetching a high-risk portfolio allocation
    Allocation high = getAllocation("low");
    // high.equity = 0.8, high.debt = 0.2, high.moneymarktet = 0.0

    // Fetching FIRE multiplier for luxury lifestyle
    static LifeStyle luxury = getLifeStyle("modest");
    // luxury.rate = 37.5 (used to calculate FIRE target)

    /**
     * Calculates the weighted expected annual return of the portfolio
     * based on selected risk allocation (equity, debt, money market).
     *
     * Formula:
     * Expected Return =
     * (Equity Allocation × Equity Return) +
     * (Debt Allocation × Debt Return) +
     * (Money Market Allocation × Money Market Return)
     *
     * @return overall expected annual portfolio return
     */
    public double getExpectedReturn() {

        double equityComponent = high.equity * equityReturn;
        double debtComponent = high.debt * debtReturn;
        double moneyMarketComponent = high.moneyMarket * moneyMarketReturn;
        return equityComponent + debtComponent + moneyMarketComponent;
    }

    /**
     * Calculates the monthly investment contribution for a given year.
     * Applies annual increment from second year onward.
     *
     * Year 1 → Base monthly investment
     * Year > 1 → Previous monthly × (1 + annualIncrement)
     *
     * @param year            current investment year
     * @param baseMonthly     starting monthly investment
     * @param previousMonthly last year's monthly investment
     * @param annualIncrement yearly increment percentage
     * @return updated monthly contribution
     */
    public static double calculateMonthlyContribution(int year, double baseMonthly, double previousMonthly,
            double annualIncrement) {
        double currentMonthly;

        if (year == 1) {
            currentMonthly = baseMonthly;
        } else {
            currentMonthly = previousMonthly * (1 + annualIncrement);
        }
        return currentMonthly;
    }

    /**
     * Calculates total annual contribution for a specific year.
     *
     * Year 1 → (monthly × 12) + initial investment
     * Other Years → (monthly × 12)
     * If retirement age exceeded → returns 0
     *
     * @param currentAge        current age in loop
     * @param retirementAge     retirement target age
     * @param monthly           monthly contribution
     * @param year              investment year
     * @param initialInvestment one-time initial investment
     * @return annual contribution amount
     */
    public static double annualContribution(int currentAge, int retirementAge, double monthly, int year,
            int initialInvestment) {

        try {
            if ((currentAge + 1) > retirementAge) {
                return 0;
            } else {

                if (year == 1) {
                    return (monthly * 12) + initialInvestment;
                } else {
                    return (monthly * 12);
                }
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Calculates cumulative investment made till current year.
     *
     * Year 1 → monthly × 12 + initial investment
     * Other Years → previous cumulative + current annual contribution
     *
     * @param age                current age
     * @param retirementAge      retirement age limit
     * @param annual             annual contribution
     * @param previousCumulative previous total invested amount
     * @param monthly            monthly investment
     * @param initialInvestment  initial lump sum
     * @param year               current investment year
     * @return total cumulative investment
     */
    public static double cummulativeContribution(int age, int retirementAge, double annual, double previousCumulative,
            double monthly, double initialInvestment, int year) {

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

    public double profit() {

        return 0;
    }

    /**
     * Calculates portfolio closing balance for a given year
     * using Future Value (FV) formula.
     *
     * Year 1:
     * - FV of initial investment (1 year)
     * - FV of 12 monthly contributions
     *
     * Year > 1:
     * - FV of previous closing balance
     * - FV of 12 monthly contributions
     *
     * @param expectedReturn         annual expected return rate
     * @param year                   current investment year
     * @param initialInvestment      one-time investment
     * @param currentMonthly         monthly investment
     * @param previousClosingBalance previous year's portfolio value
     * @return portfolio closing balance
     */
    public static double portfolio(double expectedReturn, int year, double initialInvestment, double currentMonthly,
            double previousClosingBalance) {
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

    /**
     * Calculates yearly inflation-adjusted annual expense.
     *
     * Year 1 → Monthly Expense × 12 × (1 + inflation)
     * Other Years → Previous adjusted expense × (1 + inflation)
     *
     * @param monthlyExpenses                  current monthly expense
     * @param year                             investment year
     * @param age                              current age
     * @param retirementAge                    retirement age limit
     * @param previousInflationAdjustedBalance previous adjusted expense
     * @param inflation                        annual inflation rate
     * @return inflation adjusted annual expense
     */
    public static double inflationAdjustedBalance(int monthlyExpenses, int year, int age, int retirementAge,
            double previousInflationAdjustedBalance, double inflation) {

        try {

            if (age + 1 > retirementAge) {
                return 0;
            } else {

                if (year == 1) {
                    return monthlyExpenses * 12 * inflation + monthlyExpenses * 12;
                } else {
                    return previousInflationAdjustedBalance * inflation + previousInflationAdjustedBalance;
                }
            }
        } catch (Exception e) {
            return 0;
        }

    }

    /**
     * Increments retirement age by 1 year.
     *
     * @param redAge current redemption age
     * @return next year's age
     */
    public static int getRedemptionAge(int redAge) {

        try {

            return redAge + 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Calculates the number of years passed since starting age.
     *
     * Example:
     * currentAge = 29
     * redAge = 61
     * year = 61 - 29 = 32
     *
     * @param currentAge starting age
     * @param redAge     current redemption age
     * @return year number in total timeline
     */
    public static int getYear(int currentAge, int redAge) {

        return redAge - currentAge;
    }

    /**
     * Calculates FIRE target corpus based on lifestyle multiplier.
     *
     * Formula:
     * FIRE Target = Inflation Adjusted Annual Expense × Lifestyle Rate
     *
     * @param inflationAdjustedBalance inflation-adjusted annual expense
     * @return required FIRE corpus
     */
    public static double fireAmountTarget(double inflationAdjustedBalance) {

        return inflationAdjustedBalance * luxury.rate;
    }

    /**
     * Calculates Future Value (FV) of an investment.
     * Similar to Excel FV function.
     *
     * FV formula:
     * FV = PV × (1 + r)^n + PMT × [((1 + r)^n - 1) / r]
     *
     * If type = true → payments at beginning of period
     * If type = false → payments at end of period
     *
     * @param rate interest rate per period
     * @param nper number of periods
     * @param pmt  periodic payment
     * @param pv   present value
     * @param type payment timing (true = beginning, false = end)
     * @return calculated future value
     */
    public static double fv(double rate, int nper, double pmt, double pv, boolean type) {
        MathContext mc = new MathContext(15, RoundingMode.HALF_UP);

        BigDecimal bdRate = new BigDecimal(rate, mc);
        BigDecimal bdPv = new BigDecimal(pv, mc);
        BigDecimal bdPmt = new BigDecimal(pmt, mc);

        if (rate == 0) {
            return -(bdPv.add(bdPmt.multiply(new BigDecimal(nper), mc))).doubleValue();
        } else {
            BigDecimal r1 = BigDecimal.ONE.add(bdRate, mc).round(mc);
            BigDecimal pow = r1.pow(nper, mc).round(mc);
            BigDecimal factor = pow.subtract(BigDecimal.ONE, mc)
                    .divide(bdRate, mc)
                    .round(mc);

            if (type) {
                factor = factor.multiply(r1, mc).round(mc);
            }

            BigDecimal result = bdPv.negate(mc).multiply(pow, mc)
                    .subtract(bdPmt.multiply(factor, mc), mc)
                    .round(mc);

            // RETURN WITHOUT ROUNDING
            return result.doubleValue();
        }
    }

    /**
     * Calculates annual expenses adjusted for inflation during retirement.
     *
     * First retirement year:
     * Expense = MonthlyExpense × 12 × (1 + inflation) ^
     * ((retirementage-currentage)+1)
     *
     * Next years:
     * PreviousExpense × (1 + inflation)+previousInflationAdjustedExpense
     *
     * @param MonthlyExpense                   base monthly expense
     * @param retirementAge                    retirement age
     * @param currentAge                       current age
     * @param previousInflationAdjustedExpense last year's expense
     * @param year                             current year in timeline
     * @param inflation                        annual inflation rate
     * @return yearly inflation-adjusted expense
     */
    // Calculates inflation-adjusted annual expense during retirement phase
    public static double inflationAdjustedExpense(
            int monthlyExpense,
            int retirementAge,
            int currentAge,
            int redAge,
            double previousInflationAdjustedExpense,
            double inflation) {

        try {

            // First year after retirement
            if (redAge == retirementAge + 1) {

                int power = retirementAge - currentAge + 1;

                // Inflate expense from current age to retirement age
                double result = (monthlyExpense * 12) * Math.pow((1 + inflation), power);

                return result;

            } else {

                // After first retirement year → grow previous year's expense by inflation
                return previousInflationAdjustedExpense * (1 + inflation);
            }

        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Calculates portfolio value during retirement phase.
     *
     * Logic:
     * 1. Apply yearly growth on portfolio.
     * 2. Subtract monthly withdrawals.
     *
     * First retirement year:
     * Uses valueAtRetirement as base.
     *
     * Later years:
     * Uses previousPortfolio as base.
     *
     * @param expectedReturn    annual expected return
     * @param year              current year number
     * @param valueAtRetirement portfolio at retirement
     * @param moneyWithdrawl    monthly withdrawal
     * @param previousPortfolio previous year's portfolio
     * @return updated portfolio value
     */
    // Calculates portfolio value during redemption (withdrawal) phase
    public static double redportfolio(
            double expectedReturn,
            int redAge,
            int retirementAge,
            double valueAtRetirement,
            double moneyWithdrawl,
            double previousPortfolio) {

        try {

            double part1, part2;

            // First retirement year
            if (redAge == retirementAge + 1) {

                // Grow retirement corpus for 1 year
                part1 = fv(expectedReturn, 1, 0, -valueAtRetirement, true);

            } else {

                // Grow previous year's portfolio
                part1 = fv(expectedReturn, 1, 0, -previousPortfolio, true);
            }

            // Monthly withdrawals reduce portfolio (passing as positive subtracts from
            // balance)
            part2 = fv(expectedReturn / 12.0, 12, moneyWithdrawl, 0, true);

            return part1 + part2;

        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Converts annual inflation-adjusted expense
     * into monthly withdrawal amount.
     *
     * @param inflationAdjustedExpense yearly expense
     * @return monthly withdrawal amount
     */
    public static double moneyWithdrawl(double inflationAdjustedExpense) {
        return inflationAdjustedExpense / 12;
    }

    public static void main(String[] args) {
        SpringApplication.run(FirecalculatorApplication.class, args);

        // Create object of application to access non-static methods
        FirecalculatorApplication app = new FirecalculatorApplication();

        // Calculate expected portfolio return based on selected risk allocation
        app.expectedReturn = app.getExpectedReturn();

        System.out.println("EXPECTED RETURN IS::" + app.expectedReturn);

        // These variables store previous year's values
        // so that compounding calculations can work correctly

        double previousCumulative = 0; // Total contribution till last year
        double previousClosingBalance = 0; // Portfolio value of previous year
        double previousInflationAdjustedBalance = 0;// Inflation adjusted expense of previous year
        double previousMonthly = app.monthly; // Previous monthly investment
        double previousInflationAdjustedExpense = 0;// Used in redemption phase
        double previousRedemptionPortfolio = 0; // Redemption portfolio previous year
        double profit = 0; // Profit at retirement
        double fireamount = 0; // FIRE target amount

        // Calculations Table Header
        System.out.println(
                "-----------------------------------------------------------------------------------------------------------------------------");
        System.out.printf("%65s%n", "CALCULATIONS");
        System.out.println(
                "-----------------------------------------------------------------------------------------------------------------------------");
        System.out.printf(
                "%-5s | %-5s | %-10s | %-12s | %-13s | %-13s | %-12s | %-17s | %-18s%n",
                "Age", "Year", "Monthly", "Annual", "Cumulative", "Portfolio", "Profit",
                "Infl.Adj.Balance", "Fire Amount");
        System.out.println(
                "-----------------------------------------------------------------------------------------------------------------------------");
        for (int loopAge = app.currentAge; loopAge < app.retirementAge; loopAge++) {
            // Update age and year for current iteration
            app.age = loopAge + 1;
            app.year = app.age - app.currentAge;

            // app.redempYear=app.redAge-app.currentAge;

            // Perform calculations
            double currentMonthly = calculateMonthlyContribution(app.year, app.monthly, previousMonthly,
                    app.annualIncrement);
            previousMonthly = currentMonthly;

            double annual = annualContribution(loopAge, app.retirementAge, currentMonthly, app.year,
                    app.initialInvestment);

            double cumulative = cummulativeContribution(loopAge, app.retirementAge, annual, previousCumulative,
                    app.monthly, app.initialInvestment, app.year);

            double closingBalance = portfolio(app.expectedReturn, app.year, app.initialInvestment, currentMonthly,
                    previousClosingBalance);
            profit = closingBalance - cumulative;
            double inflationAdjusted = inflationAdjustedBalance(app.monthlyExpenses, app.year, loopAge,
                    app.retirementAge, previousInflationAdjustedBalance, app.inflation);
            fireamount = fireAmountTarget(inflationAdjusted);

            System.out.printf(
                    "%-5d | %-5d | %-10d | %-12d | %-13d | %-13d | %-12d | %-17d | %-18d%n",
                    app.age,
                    app.year,
                    Math.round(currentMonthly),
                    Math.round(annual),
                    Math.round(cumulative),
                    Math.round(closingBalance),
                    Math.round(profit),
                    Math.round(inflationAdjusted),
                    Math.round(fireamount));

            // Update previous values for next iteration
            previousCumulative = cumulative;
            previousClosingBalance = closingBalance;
            previousMonthly = currentMonthly;
            previousInflationAdjustedBalance = inflationAdjusted;

        }
        // ================= Redemption Phase =================
        // After retirement, money is no longer invested monthly.
        // Instead, inflation-adjusted expenses are withdrawn from portfolio.

        app.redAge = app.retirementAge; // Start redemption from retirement age
        app.redempYear = app.retirementAge + 1 - app.currentAge; // Calculate retirement year number

        System.out.println(
                "\n----------------------------------------------------------------------------------");
        System.out.printf("%50s%n", "REDEMPTION CALCULATIONS");
        System.out.println("----------------------------------------------------------------------------------");
        System.out.printf(
                "%-5s | %-5s | %-15s | %-14s | %-14s%n",
                "Age", "Year", "Infl.Adj.Exp", "MonthlyWithdraw", "RedemptionPortfolio");
        System.out.println("----------------------------------------------------------------------------------");

        int yearsAfterRetirement = 0; // Counter for positive portfolio years

        for (int loopAge = app.redAge; loopAge < app.lifeExpectancy; loopAge++) {

            // Increase age by 1 year
            app.redAge = getRedemptionAge(app.redAge);

            // Calculate year number relative to starting age
            app.redempYear = getYear(app.currentAge, app.redAge);

            // Calculate yearly expense adjusted for inflation
            double inflationAdjustedExpense = inflationAdjustedExpense(
                    app.monthlyExpenses,
                    app.retirementAge,
                    app.currentAge,
                    app.redAge,
                    previousInflationAdjustedExpense,
                    app.inflation);

            // Convert yearly expense into monthly withdrawal
            double moneyWithdrawl = moneyWithdrawl(inflationAdjustedExpense);

            // Calculate remaining portfolio after growth and withdrawals
            double portfolio = redportfolio(
                    app.expectedReturn,
                    app.redAge,
                    app.retirementAge,
                    previousClosingBalance,
                    moneyWithdrawl,
                    previousRedemptionPortfolio);

            // Increment counter if portfolio is positive
            if (portfolio > 0) {
                yearsAfterRetirement++;
            }

            // Print redemption year data
            System.out.printf(
                    "%-5d | %-5d | %-15.0f | %-14s | %-14.0f%n",
                    app.redAge,
                    app.redempYear,
                    inflationAdjustedExpense,
                    "(" + Math.round(moneyWithdrawl) + ")",
                    portfolio);

            // Store values for next iteration
            previousInflationAdjustedExpense = inflationAdjustedExpense;
            previousRedemptionPortfolio = portfolio;
        }

        System.out.println("\n================================================================================");
        System.out.println("                                RETIREMENT SUMMARY                              ");
        System.out.println("================================================================================");
        System.out.printf("%-30s : PKR %,d%n", "Total Contribution", Math.round(previousCumulative));
        System.out.printf("%-30s : PKR %,d%n", "Profit", Math.round(profit));
        System.out.printf("%-30s : PKR %,d%n", "Value at Retirement", Math.round(previousClosingBalance));
        System.out.printf("%-30s : PKR %,d%n", "FIRE Amount at Retirement", Math.round(fireamount));
        System.out.printf("%-30s : %d%%%n", "Achieved Target", Math.round((previousClosingBalance / fireamount) * 100));
        System.out.println("================================================================================");
        System.out.println("Your Savings will Last for " + yearsAfterRetirement + " years After retirement");
        System.out.println("Based your Retirement plan and life style goals we suggest that you invest in");

        // Risk Suggestion Logic
        String lifestyleChoice = luxury.name; // Automatically uses the name from luxury config
        int yearsToRetirement = app.retirementAge - app.currentAge;
        String suggestedRisk = "";

        if (yearsToRetirement <= 10) {
            if (lifestyleChoice.equalsIgnoreCase("Luxury")) {
                suggestedRisk = "Medium Risk";
            } else {
                suggestedRisk = "Low Risk";
            }
        } else if (yearsToRetirement <= 20) {
            if (lifestyleChoice.equalsIgnoreCase("Luxury")) {
                suggestedRisk = "High Risk";
            } else {
                suggestedRisk = "Medium Risk";
            }
        } else { // > 20 years
            if (lifestyleChoice.equalsIgnoreCase("Modest")) {
                suggestedRisk = "Medium Risk";
            } else {
                suggestedRisk = "High Risk";
            }
        }

        System.out.println(suggestedRisk + " Funds");

        System.out.println("To reach your FIRE Amount you need to save "  + app.monthly + " Monthly" );

        System.out.println("================================================================================");

        //
        // System.out.println(
        // " Age :"+loopAge+
        // "| Year "+redempYear );

        // previousInflationAdjustedBalance=inflationAdjustedExpense*app.inflation+inflationAdjustedExpense;

    }

}
