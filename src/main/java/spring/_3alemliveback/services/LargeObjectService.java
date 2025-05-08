package spring._3alemliveback.services;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.postgresql.PGConnection;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LargeObjectService {

    @Autowired
    private DataSource dataSource;

    @Transactional
    public byte[] readLargeObject(long oid) throws SQLException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();

            // Obtenir le LargeObjectManager
            LargeObjectManager lobj = conn.unwrap(PGConnection.class).getLargeObjectAPI();

            // Ouvrir le LargeObject en mode lecture
            LargeObject obj = lobj.open(oid, LargeObjectManager.READ);

            // Lire les données
            byte[] data = obj.read(obj.size());

            // Fermer le LargeObject
            obj.close();

            return data;
        } finally {
            // Ne pas fermer la connexion car elle est gérée par la transaction
        }
    }

    @Transactional
    public long writeLargeObject(InputStream inputStream) throws SQLException, IOException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();

            // Obtenir le LargeObjectManager
            LargeObjectManager lobj = conn.unwrap(PGConnection.class).getLargeObjectAPI();

            // Créer un nouveau LargeObject
            long oid = lobj.createLO(LargeObjectManager.READWRITE);

            // Ouvrir le LargeObject en mode écriture
            LargeObject obj = lobj.open(oid, LargeObjectManager.WRITE);

            // Écrire les données
            byte[] buf = new byte[2048];
            int s;
            while ((s = inputStream.read(buf, 0, 2048)) > 0) {
                obj.write(buf, 0, s);
            }

            // Fermer le LargeObject
            obj.close();

            return oid;
        } catch (Exception e) {
            throw new SQLException("Erreur lors de l'écriture du Large Object", e);
        } finally {
            // Ne pas fermer la connexion car elle est gérée par la transaction
        }
    }
}