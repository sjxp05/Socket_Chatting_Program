package tests;

public class KoreanUnicodeTest {
    public static void main(String[] args) {
        System.out.println((int) '가');
        System.out.println((int) '각');
        System.out.println((int) '갂');
        System.out.println((int) '갃');
        System.out.println((int) '간');
        System.out.println((int) '갅');
        System.out.println((int) '갆');
        System.out.println((int) '갇');
        System.out.println((int) '갈');
        System.out.println((int) '갉');

        System.out.println();
        System.out.println((int) ' ');
        System.out.println();

        for (int i = 32; i < 128; i++) {
            System.out.print((char) i + " ");
        }
    }
}
