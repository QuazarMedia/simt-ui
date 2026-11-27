"""Local, stored ZIP: fixed metadata, no network or upstream execution."""
import hashlib
import json
import pathlib
import stat
import sys
import zipfile


def archive(request):
    root = pathlib.Path(request["root"])
    destination = root / "build" / "distributions" / "cosmos-2.3.0-private.zip"
    for path in [root, destination.parent.parent, destination.parent]:
        if path.is_symlink():
            raise ValueError("Archive output directory cannot be a symlink")
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.is_symlink():
        raise ValueError("Archive destination cannot be a symlink")
    reviewed = request["entries"]
    if [entry["path"] for entry in reviewed] != sorted(set(entry["path"] for entry in reviewed)):
        raise ValueError("Archive entries must be sorted and unique")
    with zipfile.ZipFile(destination, "w", compression=zipfile.ZIP_STORED) as output:
        for entry in reviewed:
            relative = pathlib.PurePosixPath(entry["path"])
            if relative.is_absolute() or ".." in relative.parts:
                raise ValueError("Invalid archive path")
            source = root / relative
            if any(path.is_symlink() for path in [source, *source.parents]):
                raise ValueError("Archive source cannot traverse a symlink")
            data = source.read_bytes()
            if hashlib.sha256(data).hexdigest() != entry["sha256"]:
                raise ValueError("Archive bytes changed after integrity check")
            info = zipfile.ZipInfo(entry["path"], date_time=(1980, 1, 1, 0, 0, 0))
            info.create_system = 3
            info.external_attr = (stat.S_IFREG | 0o644) << 16
            output.writestr(info, data)
    print(json.dumps({"archive": str(destination), "entries": len(reviewed), "sha256": hashlib.sha256(destination.read_bytes()).hexdigest()}))


if __name__ == "__main__":
    archive(json.load(sys.stdin))
