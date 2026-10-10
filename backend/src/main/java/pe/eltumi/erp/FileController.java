package pe.eltumi.erp;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static pe.eltumi.erp.Store.*;
@RestController @RequestMapping("/api")
public class FileController {
 final Store db;final Access access;final Catalog catalog;final Path directory;
 public FileController(Store d,Access a,Catalog c,@Value("${app.files}")String path)throws Exception{db=d;access=a;catalog=c;directory=Path.of(path).toAbsolutePath().normalize();Files.createDirectories(directory);}
 @PostMapping("/files") public Object upload(@RequestParam MultipartFile file,@RequestParam String module)throws Exception{access.module(module,"write");byte[] bytes=file.getBytes();require(bytes.length>0&&bytes.length<=10*1024*1024,"El archivo debe tener entre 1 byte y 10 MB");String mime=bytes.length>=5&&new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-")?"application/pdf":bytes.length>=8&&bytes[0]==(byte)137&&bytes[1]==80&&bytes[2]==78&&bytes[3]==71?"image/png":bytes.length>=3&&bytes[0]==(byte)255&&bytes[1]==(byte)216&&bytes[2]==(byte)255?"image/jpeg":"";require(!mime.isBlank(),"Solo se aceptan PDF, PNG y JPEG");String id=UUID.randomUUID().toString();String original=Objects.requireNonNullElse(file.getOriginalFilename(),"archivo").replaceAll("[\\\\/\\r\\n\\x00]","_");if(original.length()>180)original=original.substring(0,180);Files.write(directory.resolve(id),bytes,StandardOpenOption.CREATE_NEW);try{db.exec("INSERT INTO files(id,original_name,mime,module,owner_id,size_bytes) VALUES(?,?,?,?,?,?)",id,original,mime,module,Security.user().id(),bytes.length);}catch(Exception e){Files.deleteIfExists(directory.resolve(id));throw e;}return map("id",id,"name",original,"mime",mime);}
 @GetMapping("/files/{id}")public ResponseEntity<byte[]> download(@PathVariable String id)throws Exception{require(id.matches("[a-f0-9-]{36}"),"Archivo inválido");var f=db.one("SELECT * FROM files WHERE id=?",id);access.module(str(f,"module"),"read");var path=directory.resolve(id);if(!Files.exists(path))throw new Problem(404,"El archivo no está disponible en el almacenamiento configurado");return ResponseEntity.ok().contentType(MediaType.parseMediaType(str(f,"mime"))).header("Content-Disposition",ContentDisposition.attachment().filename(str(f,"original_name"),StandardCharsets.UTF_8).build().toString()).header("X-Content-Type-Options","nosniff").body(Files.readAllBytes(path));}
 @PostMapping("/attachments/{entity}/{id}")@Transactional public void attach(@PathVariable String entity,@PathVariable long id,@RequestBody Map<String,Object>b){access.entity(entity,"write");db.lock("company",1);require(catalog.fields(entity).stream().anyMatch(f->str(f,"key").equals("file_id")),"Este registro no admite un documento");String file=str(b,"file_id");var f=db.one("SELECT * FROM files WHERE id=?",file);access.module(str(f,"module"),"read");db.get(entity,id);db.update(entity,id,map("file_id",file));access.audit("ADJUNTO",entity,id,"Documento actualizado");}
}
