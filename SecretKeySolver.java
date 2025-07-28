import java.io.*;
import java.math.BigInteger;
import java.util.*;
import org.json.JSONObject;

public class SecretKeySolver {

    static class Point {
        BigInteger x;
        BigInteger y;

        Point(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }

    static BigInteger decode(String value, int base) {
        return new BigInteger(value, base);
    }

    static BigInteger[][] buildMatrix(List<Point> points, int k) {
        BigInteger[][] matrix = new BigInteger[k][k];
        for (int i = 0; i < k; i++) {
            BigInteger x = points.get(i).x;
            BigInteger power = BigInteger.ONE;
            for (int j = 0; j < k; j++) {
                matrix[i][j] = power;
                power = power.multiply(x);
            }
        }
        return matrix;
    }

    static BigInteger[] gaussianSolve(BigInteger[][] A, BigInteger[] b) {
        int n = A.length;
        BigInteger[] x = new BigInteger[n];

        for (int i = 0; i < n; i++) {
            BigInteger divisor = A[i][i];
            for (int j = i; j < n; j++) {
                A[i][j] = A[i][j].divide(divisor);
            }
            b[i] = b[i].divide(divisor);

            for (int k = i + 1; k < n; k++) {
                BigInteger factor = A[k][i];
                for (int j = i; j < n; j++) {
                    A[k][j] = A[k][j].subtract(factor.multiply(A[i][j]));
                }
                b[k] = b[k].subtract(factor.multiply(b[i]));
            }
        }

        for (int i = n - 1; i >= 0; i--) {
            BigInteger sum = BigInteger.ZERO;
            for (int j = i + 1; j < n; j++) {
                sum = sum.add(A[i][j].multiply(x[j]));
            }
            x[i] = b[i].subtract(sum);
        }

        return x;
    }

    static List<List<Point>> generateCombinations(List<Point> points, int k) {
        List<List<Point>> result = new ArrayList<>();
        combine(points, new ArrayList<>(), 0, k, result);
        return result;
    }

    static void combine(List<Point> points, List<Point> temp, int start, int k, List<List<Point>> result) {
        if (temp.size() == k) {
            result.add(new ArrayList<>(temp));
            return;
        }
        for (int i = start; i < points.size(); i++) {
            temp.add(points.get(i));
            combine(points, temp, i + 1, k, result);
            temp.remove(temp.size() - 1);
        }
    }

    static BigInteger[] findValidPolynomial(List<Point> allPoints, int k, List<Point> validComboOut) {
        List<List<Point>> combinations = generateCombinations(allPoints, k);

        for (List<Point> combo : combinations) {
            try {
                BigInteger[][] A = buildMatrix(combo, k);
                BigInteger[] b = new BigInteger[k];
                for (int i = 0; i < k; i++) {
                    b[i] = combo.get(i).y;
                }
                BigInteger[] coeffs = gaussianSolve(A, b);

                boolean valid = true;
                for (int i = 0; i < k; i++) {
                    BigInteger xVal = combo.get(i).x;
                    BigInteger expectedY = combo.get(i).y;
                    BigInteger actualY = BigInteger.ZERO;
                    BigInteger xPower = BigInteger.ONE;
                    for (BigInteger coeff : coeffs) {
                        actualY = actualY.add(coeff.multiply(xPower));
                        xPower = xPower.multiply(xVal);
                    }
                    if (!actualY.equals(expectedY)) {
                        valid = false;
                        break;
                    }
                }

                if (valid) {
                    validComboOut.addAll(combo);
                    return coeffs;
                }

            } catch (Exception e) {
                
            }
        }

        return null;
    }

    public static void main(String[] args) throws Exception {
        String[] files = {"testcase1.json", "testcase2.json"};

        for (String file : files) {
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(file)));
            JSONObject json = new JSONObject(content);

            JSONObject keys = json.getJSONObject("keys");
            int k = keys.getInt("k");

            List<Point> points = new ArrayList<>();

            for (String key : json.keySet()) {
                if (key.equals("keys")) continue;

                JSONObject entry = json.getJSONObject(key);
                int base = Integer.parseInt(entry.getString("base"));
                String value = entry.getString("value");

                BigInteger x = new BigInteger(key);
                BigInteger y = decode(value, base);

                points.add(new Point(x, y));
            }

            List<Point> validCombo = new ArrayList<>();
            BigInteger[] coeffs = findValidPolynomial(points, k, validCombo);

            if (coeffs != null) {
                System.out.println("File: " + file);
                for (int i = 0; i < coeffs.length; i++) {
                    System.out.println("Coefficient a_" + i + " = " + coeffs[i]);
                }
                System.out.println("Secret (constant term c): " + coeffs[0]);

               
                String outputFile = "correct_keys_" + file.replace(".json", "") + ".txt";
                try (PrintWriter writer = new PrintWriter(outputFile)) {
                    for (int i = 0; i < coeffs.length; i++) {
                        writer.println("Coefficient a_" + i + " = " + coeffs[i]);
                    }
                    writer.println("Secret (constant term c): " + coeffs[0]);
                }
                System.out.println("Correct keys saved to: " + outputFile);

            
                Set<BigInteger> validX = new HashSet<>();
                for (Point p : validCombo) validX.add(p.x);

                System.out.println("Wrong keys:");
                for (Point p : points) {
                    if (!validX.contains(p.x)) {
                        System.out.println("x = " + p.x + ", y = " + p.y);
                    }
                }

                System.out.println("-----");
            } else {
                System.out.println("No valid polynomial found in " + file);
            }
        }
    }
}
