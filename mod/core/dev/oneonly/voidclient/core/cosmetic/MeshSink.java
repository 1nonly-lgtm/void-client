package dev.oneonly.voidclient.core.cosmetic;

/**
 * Wyjście geometrii kosmetyków do gry. Współrzędne w pikselach modelu gracza (osie jak w modelu Minecrafta:
 * y w dół, przód postaci w stronę -z, początek w szyi). Adapter wersji dzieli przez 16 i mnoży przez macierz.
 */
public interface MeshSink {
    /** Tryby: 0 – oświetlony, 1 – świecący, 2 – świecący addytywnie (poświata). */
    void begin(String texture, int mode);

    void vertex(float x, float y, float z, float u, float v, int argb, float nx, float ny, float nz);

    void end();
}
