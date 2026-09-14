from runpy import run_path

run_path("hybrid/apply_batch_export_core.py", run_name="__main__")
run_path("hybrid/apply_gallery_rotation_refinement.py", run_name="__main__")
run_path("hybrid/apply_single_output_viewmodel.py", run_name="__main__")
run_path("hybrid/apply_single_output_ui.py", run_name="__main__")

print("Applied batch export, gallery rotation, and opened-photo output refinements.")
