/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: /home/fatma/Android/Sdk/build-tools/35.0.0/aidl -p/home/fatma/Android/Sdk/platforms/android-35/framework.aidl -o/home/fatma/High-end-Luxury-Multi-Display-User/App/DriverLauncher/app/build/generated/aidl_source_output_dir/debug/out -I/home/fatma/High-end-Luxury-Multi-Display-User/App/DriverLauncher/app/src/main/aidl -I/home/fatma/High-end-Luxury-Multi-Display-User/App/DriverLauncher/app/src/debug/aidl -I/home/fatma/.gradle/caches/8.11.1/transforms/583bb7f9b1eb422dbe30b44ba09d89fa/transformed/core-1.16.0/aidl -I/home/fatma/.gradle/caches/8.11.1/transforms/fe6c723262a7bbddef3278f3d61d872e/transformed/versionedparcelable-1.1.1/aidl -d/tmp/aidl12700468768511308547.d /home/fatma/High-end-Luxury-Multi-Display-User/App/DriverLauncher/app/src/main/aidl/com/example/driverlauncher/IGpsService.aidl
 */
package com.example.driverlauncher;
// Declare any non-default types here with import statements
public interface IGpsService extends android.os.IInterface
{
  /** Default implementation for IGpsService. */
  public static class Default implements com.example.driverlauncher.IGpsService
  {
    @Override public double getLatitude() throws android.os.RemoteException
    {
      return 0.0d;
    }
    @Override public double getLongitude() throws android.os.RemoteException
    {
      return 0.0d;
    }
    @Override public float getSpeed() throws android.os.RemoteException
    {
      return 0.0f;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.example.driverlauncher.IGpsService
  {
    /** Construct the stub at attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.example.driverlauncher.IGpsService interface,
     * generating a proxy if needed.
     */
    public static com.example.driverlauncher.IGpsService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.example.driverlauncher.IGpsService))) {
        return ((com.example.driverlauncher.IGpsService)iin);
      }
      return new com.example.driverlauncher.IGpsService.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
      }
      switch (code)
      {
        case TRANSACTION_getLatitude:
        {
          double _result = this.getLatitude();
          reply.writeNoException();
          reply.writeDouble(_result);
          break;
        }
        case TRANSACTION_getLongitude:
        {
          double _result = this.getLongitude();
          reply.writeNoException();
          reply.writeDouble(_result);
          break;
        }
        case TRANSACTION_getSpeed:
        {
          float _result = this.getSpeed();
          reply.writeNoException();
          reply.writeFloat(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.example.driverlauncher.IGpsService
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public double getLatitude() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        double _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getLatitude, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readDouble();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public double getLongitude() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        double _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getLongitude, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readDouble();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public float getSpeed() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        float _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getSpeed, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readFloat();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_getLatitude = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getLongitude = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_getSpeed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "com.example.driverlauncher.IGpsService";
  public double getLatitude() throws android.os.RemoteException;
  public double getLongitude() throws android.os.RemoteException;
  public float getSpeed() throws android.os.RemoteException;
}
