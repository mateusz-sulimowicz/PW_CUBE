import concurrentcube.Cube;

public class Validate {

    private static final String EXPECTED =
              "0000"
            + "0000"
            + "0000"
            + "1111"

            + "1115"
            + "1115"
            + "4444"
            + "1115"

            + "2222"
            + "2222"
            + "1115"
            + "2222"

            + "0333"
            + "0333"
            + "2222"
            + "0333"

            + "4444"
            + "4444"
            + "0333"
            + "4444"

            + "3333"
            + "5555"
            + "5555"
            + "5555";

    private static void error(int code) {
        System.out.println("ERROR " + code);
        System.exit(code);
    }

    public static void main(String[] args) {
        var counter = new Object() { int value = 0; };

        Cube cube = new Cube(4,
                (x, y) -> { ++counter.value; },
                (x, y) -> { ++counter.value; },
                () -> { ++counter.value; },
                () -> { ++counter.value; }
        );

        try {

            cube.rotate(2, 0);
            cube.rotate(5, 1);

            if (counter.value != 4) {
                error(1);
            }

            String state = cube.show();

            if (counter.value != 6) {
                error(2);
            }

            if (!state.equals(EXPECTED)) {
                error(3);
            }

            System.out.println("OK");

        } catch (InterruptedException e) {
            error(4);
        }
    }

}
