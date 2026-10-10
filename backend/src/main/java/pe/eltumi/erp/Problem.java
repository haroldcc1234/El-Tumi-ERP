package pe.eltumi.erp;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.util.Map;
public class Problem extends RuntimeException {public final int status;public Problem(int status,String message){super(message);this.status=status;}}
@RestControllerAdvice
class Errors {
 @ExceptionHandler(Problem.class) ResponseEntity<?> problem(Problem e){return ResponseEntity.status(e.status).body(Map.of("message",e.getMessage()));}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> integrity(DataIntegrityViolationException e){return ResponseEntity.status(409).body(Map.of("message","El registro ya existe o está relacionado con otros movimientos. Revisa los códigos y referencias."));}
 @ExceptionHandler({IllegalArgumentException.class,org.springframework.http.converter.HttpMessageNotReadableException.class}) ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().body(Map.of("message","Datos inválidos. Revisa fechas, montos y campos obligatorios."));}
 @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<?> size(Exception e){return ResponseEntity.badRequest().body(Map.of("message","El archivo no debe superar 10 MB."));}
}
