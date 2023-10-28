package com.example.adminapphotel.model;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Firebase {
    private int uploadCount = 0;
    private ArrayList<String> uploadedImageUrls = new ArrayList<>();
    FirebaseFirestore mfirestore;
    FirebaseAuth mfirebaseAuth;
    FirebaseUser mfirebaseUser;
    FirebaseStorage mfirebaseStorage;
    StorageReference mstorageRef;
    Context mcontext;
    public Firebase(Context context) {
        mfirestore = FirebaseFirestore.getInstance();
        mfirebaseAuth = FirebaseAuth.getInstance();
        mfirebaseStorage = FirebaseStorage.getInstance();
        mstorageRef = mfirebaseStorage.getReference();
        this.mcontext = context;
    }
    public interface SignInCallback {
        void onCallback(boolean isSuccess, String adminID);
    }
    public interface SetDataUserCallback {
        void onCallback(boolean isSuccess, Admin admin);
    }
    public interface UpdateUserCallback {
        void onCallback(boolean isSuccess);
    }
    public interface getAllUsersCallback {
        void onCallback(boolean isSuccess, ArrayList<User> userList);
    }
    public interface DeleteRoomCallback {
        void onCallback(boolean isSuccess);
    }
    public interface getAllRoomsCallback {
        void onCallback(boolean isSuccess, ArrayList<Room> roomList);
    }
    public interface AddCarCallback {
        void onCallback(boolean isSuccess);
    }
    public interface UpdateCarCallback {
        void onCallback(boolean isSuccess);
    }
    public interface UpdateStatusOrderCallback {
        void onCallback(boolean isSuccess);
    }
    public interface getAllNewOrdersCallback {
        void onCallback(ArrayList<Order> ordersList);
    }
    public interface RoomIDCallback {
        void onCallback(int roomID);
    }
    public void signIn(String email, String password, SignInCallback callback) {
        mfirebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener((Activity) mcontext, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            mfirebaseUser = mfirebaseAuth.getCurrentUser();
                            checkExistsAdmin(mfirebaseUser.getUid(), email, password, callback);
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            callback.onCallback(false, null);
                        }
                    }
                });
    }
    private void checkExistsAdmin(String adminID, String email, String password, SignInCallback callback) {
        mfirestore.collection("admin")
                .document(adminID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                callback.onCallback(true, adminID);
                            } else {
                                saveDocumentAdminId(adminID, email, password, callback);
                            }
                        } else {
                            Log.d("TrangChuActivity", "get failed with ", task.getException());
                            callback.onCallback(false, null);
                        }
                    }
                });
    }
    private void saveDocumentAdminId(String adminID, String email, String password, SignInCallback callback){
        Map<String, Object> admin = new HashMap<>();
        admin.put("email", email);
        admin.put("password", password);

        // Add a new document with a specific ID (userID)
        mfirestore.collection("admin").document(adminID)
                .set(admin)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                        callback.onCallback(true, adminID);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                        callback.onCallback(false, null);
                    }
                });
    }
    public void setDataUser(String adminID, SetDataUserCallback callback){
        mfirestore.collection("admin")
                .document(adminID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Admin admin = new Admin(document.getString("email"),
                                        document.getString("password"),
                                        document.getString("phonenumber"),
                                        document.getString("username"),
                                        document.getString("image"));
                                callback.onCallback(true, admin);
                            } else {
                                callback.onCallback(false, null);
                            }
                        } else {
                            callback.onCallback(false, null);
                        }
                    }
                });
    }
    public void updateUser(String adminID, String email, String username, String phoneNumber, UpdateUserCallback callback) {
        Map<String, Object> admin = new HashMap<>();
        admin.put("email", email);
        admin.put("username", username);
        admin.put("numberphone", phoneNumber);

        mfirestore.collection("admin").document(adminID).update(admin)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        callback.onCallback(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onCallback(false);
                    }
                });
    }
    public void updateInforAndImageUser(String adminID, String email, String username, String phoneNumber, Uri imageUri, UpdateUserCallback callback) {
        final StorageReference imgRef = mstorageRef.child("images/" + UUID.randomUUID().toString());
        imgRef.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        imgRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Map<String, Object> admin = new HashMap<>();
                                admin.put("email", email);
                                admin.put("username", username);
                                admin.put("numberphone", phoneNumber);
                                admin.put("image", uri.toString());

                                mfirestore.collection("admin").document(adminID).update(admin)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                callback.onCallback(true);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                callback.onCallback(false);
                                            }
                                        });
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onCallback(false);
                    }
                });
    }
    public void getAllUsers(getAllUsersCallback callback) {
        mfirestore.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<User> userList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = new User(
                                    document.getId(),
                                    document.getString("username"),
                                    document.getString("numberphone"),
                                    document.getString("address"),
                                    document.getString("giaypheplaixe"),
                                    document.getString("dateofbirth"),
                                    document.getString("gioitinh"),
                                    document.getString("image")
                            );
                            userList.add(user);
                        }
                        callback.onCallback(true, userList);
                    } else {
                        callback.onCallback(false, null);
                    }
                });
    }
    public void deleteCar(Room room, DeleteRoomCallback callback) {
        mfirestore.collection("rooms").document(room.getRoomID()).delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        callback.onCallback(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onCallback(false);
                    }
                });
    }
    public void getAllCars(getAllRoomsCallback callback) {
        mfirestore.collection("rooms")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<Room> roomList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Room room = new Room(document.getId(),
                                    document.getString("nameroom"),
                                    document.getDouble("priceroom"),
                                    document.getString("typeroom"),
                                    document.getString("statusroom"),
                                    document.getString("descriptionroom"),
                                    document.getString("image1"),
                                    document.getString("image2"),
                                    document.getString("image3"));
                            roomList.add(room);
                        }
                        callback.onCallback(true, roomList);
                    } else {
                        callback.onCallback(false, null);
                    }
                });
    }
    public void addCar(Room room, List<Uri> imageUris, AddCarCallback callback) {
        if(uploadCount < imageUris.size()){
            Uri imageUri = imageUris.get(uploadCount);
            final StorageReference imgRef = mstorageRef.child("images/" + UUID.randomUUID().toString());
            imgRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                imgRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    uploadedImageUrls.add(uri.toString());
                    uploadCount++;
                    addCar(room, imageUris, callback); //recursive call
                });
            }).addOnFailureListener(e -> callback.onCallback(false));
        }else{
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("nameroom", room.getNameroom());
            roomInfo.put("priceroom", room.getPriceroom());
            roomInfo.put("typeroomr", room.getTyperoom());
            roomInfo.put("statusroom", room.getStatusroom());
            roomInfo.put("descriptionroom", room.getDescriptionroom());
            if (uploadedImageUrls.size() > 0) roomInfo.put("image1", uploadedImageUrls.get(0));
            if (uploadedImageUrls.size() > 1) roomInfo.put("image2", uploadedImageUrls.get(1));
            if (uploadedImageUrls.size() > 2) roomInfo.put("image3", uploadedImageUrls.get(2));

            mfirestore.collection("cars").add(roomInfo)
                    .addOnSuccessListener(documentReference -> {
                        callback.onCallback(true);
                    })
                    .addOnFailureListener(e -> {
                        callback.onCallback(false);
                    });
        }
    }
    public void updateCar(Room room, List<Uri> imageUris, UpdateCarCallback callback) {
        if(uploadCount < imageUris.size()){
            Uri imageUri = imageUris.get(uploadCount);
            final StorageReference imgRef = mstorageRef.child("images/" + UUID.randomUUID().toString());
            imgRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                imgRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    uploadedImageUrls.add(uri.toString());
                    uploadCount++;
                    updateCar(room, imageUris, callback); //recursive call
                });
            }).addOnFailureListener(e -> callback.onCallback(false));
        }else{
            Map<String, Object> carInfo = new HashMap<>();
            carInfo.put("nameroom", room.getNameroom());
            carInfo.put("priceroom", room.getPriceroom());
            carInfo.put("typeroom", room.getTyperoom());
            carInfo.put("statusroom", room.getStatusroom());
            carInfo.put("descriptionroom", room.getDescriptionroom());
            if (uploadedImageUrls.size() > 0) carInfo.put("image1", uploadedImageUrls.get(0));
            if (uploadedImageUrls.size() > 1) carInfo.put("image2", uploadedImageUrls.get(1));
            if (uploadedImageUrls.size() > 2) carInfo.put("image3", uploadedImageUrls.get(2));

            mfirestore.collection("rooms").document(room.getRoomID()).update(carInfo)
                    .addOnSuccessListener(aVoid -> callback.onCallback(true))
                    .addOnFailureListener(e -> callback.onCallback(false));
        }
    }
    public void updateStatusOrder(String orderID,String status, UpdateStatusOrderCallback callback) {
        Map<String, Object> morder = new HashMap<>();
        morder.put("orderStatus", status);
        mfirestore.collection("orders")
                .document(orderID)
                .update(morder)
                .addOnSuccessListener(documentReference -> {
                    callback.onCallback(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving ReasonsCancel: " + e.getMessage());
                    callback.onCallback(false);
                });
    }
    public void getAllNewOrders(getAllNewOrdersCallback callback) {
        mfirestore.collection("orders")
                .whereEqualTo("orderStatus", "Đang chờ xác nhận")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<Order> ordersList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Order order = new Order(document.getId(),
                                    document.getString("userID"),
                                    document.getString("orderDate"),
                                    document.getString("orderStatus"),
                                    document.getString("rentDate"),
                                    document.getString("returnDate"),
                                    document.getString("renterName"),
                                    document.getString("renterPhone"),
                                    document.getString("roomID"),
                                    document.getString("roomName"),
                                    document.getDouble("roomPrice"),
                                    document.getString("roomType"),
                                    document.getString("paymentMethod"),
                                    document.getDouble("totalprice"));
                            ordersList.add(order);
                        }
                        callback.onCallback(ordersList);
                    } else {
                        callback.onCallback(null);
                    }
                });
    }
    public void getAllRentingOrders(getAllNewOrdersCallback callback) {
        mfirestore.collection("orders")
                .whereEqualTo("orderStatus", "Đang thuê")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<Order> ordersList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Order order = new Order(document.getId(),
                                    document.getString("userID"),
                                    document.getString("orderDate"),
                                    document.getString("orderStatus"),
                                    document.getString("rentDate"),
                                    document.getString("returnDate"),
                                    document.getString("renterName"),
                                    document.getString("renterPhone"),
                                    document.getString("roomID"),
                                    document.getString("roomName"),
                                    document.getDouble("roomPrice"),
                                    document.getString("roomType"),
                                    document.getString("paymentMethod"),
                                    document.getDouble("totalprice"));
                            ordersList.add(order);
                        }
                        callback.onCallback(ordersList);
                    } else {
                        callback.onCallback(null);
                    }
                });
    }
    public void getAllRentedOrders(getAllNewOrdersCallback callback) {
        mfirestore.collection("orders")
                .whereEqualTo("orderStatus", "Đã hoàn thành")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<Order> ordersList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Order order = new Order(document.getId(),
                                    document.getString("userID"),
                                    document.getString("orderDate"),
                                    document.getString("orderStatus"),
                                    document.getString("rentDate"),
                                    document.getString("returnDate"),
                                    document.getString("renterName"),
                                    document.getString("renterPhone"),
                                    document.getString("roomID"),
                                    document.getString("roomName"),
                                    document.getDouble("roomPrice"),
                                    document.getString("roomType"),
                                    document.getString("paymentMethod"),
                                    document.getDouble("totalprice"));
                            ordersList.add(order);
                        }
                        callback.onCallback(ordersList);
                    } else {
                        callback.onCallback(null);
                    }
                });
    }
    public void updateStatusRoom(String roomID,String status, UpdateStatusOrderCallback callback) {
        Map<String, Object> room = new HashMap<>();
        room.put("statusroom", status);
        mfirestore.collection("rooms")
                .document(roomID)
                .update(room)
                .addOnSuccessListener(documentReference -> {
                    callback.onCallback(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving ReasonsCancel: " + e.getMessage());
                    callback.onCallback(false);
                });
    }

}
